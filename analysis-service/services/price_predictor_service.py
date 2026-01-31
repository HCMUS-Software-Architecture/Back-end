"""
Price Prediction Service using Google Gemini AI.

This service combines historical price data and news sentiment
to predict short-term price direction (UP/DOWN/NEUTRAL).
"""

import logging
import pandas as pd
import numpy as np
from datetime import datetime, timedelta
from typing import Dict, List, Optional
from google import genai
from google.genai import types

from config.settings import get_settings
from models.schemas import (
    PricePredictionRequest,
    PricePredictionResponse,
    TechnicalIndicators,
    SentimentSummary
)

logger = logging.getLogger(__name__)


class PricePredictorService:
    """Predicts price direction using AI and technical/sentiment analysis."""
    
    SYSTEM_PROMPT = """You are an expert cryptocurrency market analyst with deep knowledge of technical analysis and market sentiment.

Your task is to predict the short-term price direction (next 24 hours) based on:
1. Historical price data and technical indicators
2. Recent news sentiment analysis

Be objective, data-driven, and provide clear reasoning. Consider both bullish and bearish factors.

IMPORTANT: Respond ONLY with valid JSON in this exact format:
{
  "prediction": "UP" | "DOWN" | "NEUTRAL",
  "confidence": 0.XX,
  "reasoning": "Brief explanation combining technical and sentiment factors",
  "key_factors": ["factor1", "factor2", "factor3"],
  "risk_factors": ["risk1", "risk2"]
}
"""
    
    def __init__(self):
        settings = get_settings()
        self.client = genai.Client(api_key=settings.GEMINI_API_KEY)
        self.primary_model = settings.GEMINI_MODEL
        self.fallback_model = settings.GEMINI_FALLBACK_MODEL
    
    def calculate_technical_indicators(self, candles: List[Dict]) -> TechnicalIndicators:
        """
        Calculate technical indicators from price candles.
        
        Args:
            candles: List of OHLCV dicts sorted by time (oldest first)
        
        Returns:
            TechnicalIndicators object
        """
        if len(candles) < 50:
            raise ValueError(f"Need at least 50 candles, got {len(candles)}")
        
        df = pd.DataFrame(candles)
        
        # RSI (14-period)
        delta = df['close'].diff()
        gain = (delta.where(delta > 0, 0)).rolling(window=14).mean()
        loss = (-delta.where(delta < 0, 0)).rolling(window=14).mean()
        rs = gain / loss
        rsi = 100 - (100 / (1 + rs))
        current_rsi = float(rsi.iloc[-1])
        
        # MACD (12, 26, 9)
        ema12 = df['close'].ewm(span=12, adjust=False).mean()
        ema26 = df['close'].ewm(span=26, adjust=False).mean()
        macd_line = ema12 - ema26
        signal_line = macd_line.ewm(span=9, adjust=False).mean()
        macd_histogram = macd_line - signal_line
        
        macd_signal = "bullish" if float(macd_histogram.iloc[-1]) > 0 else "bearish"
        
        # Moving Averages
        ma20 = float(df['close'].rolling(window=20).mean().iloc[-1])
        ma50 = float(df['close'].rolling(window=50).mean().iloc[-1])
        current_price = float(df['close'].iloc[-1])
        
        # Volume Analysis
        avg_volume = float(df['volume'].rolling(window=7).mean().iloc[-1])
        current_volume = float(df['volume'].iloc[-1])
        volume_change_pct = ((current_volume / avg_volume) - 1) * 100
        
        # Price Change (24h)
        if len(df) >= 24:
            price_change_24h = ((current_price / float(df['close'].iloc[-24])) - 1) * 100
        else:
            price_change_24h = ((current_price / float(df['close'].iloc[0])) - 1) * 100
        
        # RSI interpretation
        if current_rsi < 30:
            rsi_interpretation = "oversold"
        elif current_rsi > 70:
            rsi_interpretation = "overbought"
        else:
            rsi_interpretation = "neutral"
        
        return TechnicalIndicators(
            current_price=current_price,
            price_change_24h=round(price_change_24h, 2),
            high_24h=float(df['high'].tail(24).max() if len(df) >= 24 else df['high'].max()),
            low_24h=float(df['low'].tail(24).min() if len(df) >= 24 else df['low'].min()),
            rsi=round(current_rsi, 1),
            rsi_interpretation=rsi_interpretation,
            macd_signal=macd_signal,
            macd_value=round(float(macd_histogram.iloc[-1]), 2),
            ma20=round(ma20, 2),
            ma50=round(ma50, 2),
            price_vs_ma20="above" if current_price > ma20 else "below",
            price_vs_ma50="above" if current_price > ma50 else "below",
            volume_change_pct=round(volume_change_pct, 1),
            volume_trend="high" if volume_change_pct > 20 else "normal"
        )
    
    def aggregate_sentiment(self, sentiments: List[Dict]) -> SentimentSummary:
        """
        Aggregate sentiment data from recent news.
        
        Args:
            sentiments: List of sentiment documents from last 24h
        
        Returns:
            SentimentSummary object
        """
        if not sentiments:
            return SentimentSummary(
                articles_count=0,
                average_score=0.0,
                bullish_count=0,
                bearish_count=0,
                neutral_count=0,
                overall_sentiment="NEUTRAL",
                sentiment_trend="stable",
                top_rationales=[]
            )
        
        # Count categories
        bullish = sum(1 for s in sentiments if s.get('category') == 'Bullish')
        bearish = sum(1 for s in sentiments if s.get('category') == 'Bearish')
        neutral = sum(1 for s in sentiments if s.get('category') == 'Neutral')
        
        # Calculate weighted average score (more recent = higher weight)
        now = datetime.utcnow()
        total_weight = 0
        weighted_sum = 0
        
        for sentiment in sentiments:
            age_hours = (now - sentiment.get('analyzed_at', now)).total_seconds() / 3600
            weight = np.exp(-age_hours / 12)  # Exponential decay (half-life = 12h)
            weighted_sum += sentiment.get('score', 0) * weight
            total_weight += weight
        
        avg_score = weighted_sum / total_weight if total_weight > 0 else 0.0
        
        # Determine overall sentiment
        if avg_score > 0.3:
            overall = "BULLISH"
        elif avg_score < -0.3:
            overall = "BEARISH"
        else:
            overall = "NEUTRAL"
        
        # Sentiment trend (compare first half vs second half)
        mid = len(sentiments) // 2
        recent_avg = np.mean([s.get('score', 0) for s in sentiments[:mid]]) if mid > 0 else 0
        older_avg = np.mean([s.get('score', 0) for s in sentiments[mid:]]) if len(sentiments) > mid else 0
        
        if recent_avg > older_avg + 0.2:
            trend = "improving"
        elif recent_avg < older_avg - 0.2:
            trend = "declining"
        else:
            trend = "stable"
        
        # Top 3 most recent rationales
        top_rationales = [s.get('rationale', '') for s in sentiments[:3] if s.get('rationale')]
        
        return SentimentSummary(
            articles_count=len(sentiments),
            average_score=round(avg_score, 2),
            bullish_count=bullish,
            bearish_count=bearish,
            neutral_count=neutral,
            overall_sentiment=overall,
            sentiment_trend=trend,
            top_rationales=top_rationales
        )
    
    def build_prediction_prompt(
        self,
        symbol: str,
        technical: TechnicalIndicators,
        sentiment: SentimentSummary
    ) -> str:
        """Build the user prompt for AI prediction."""
        
        prompt = f"""Analyze {symbol} trend for the next 24 hours.

**Price Data (Last 100 candles):**
Current Price: ${technical.current_price:,.2f}
24h Change: {technical.price_change_24h:+.2f}%
24h High: ${technical.high_24h:,.2f}
24h Low: ${technical.low_24h:,.2f}

**Technical Indicators:**
RSI(14): {technical.rsi:.1f} ({technical.rsi_interpretation})
MACD: {technical.macd_signal.capitalize()} signal (value: {technical.macd_value:.2f})
MA20: ${technical.ma20:,.2f} (price is {technical.price_vs_ma20})
MA50: ${technical.ma50:,.2f} (price is {technical.price_vs_ma50})
Volume: {technical.volume_change_pct:+.1f}% vs 7-day average ({technical.volume_trend})

**News Sentiment (Last 24h):**
Articles Analyzed: {sentiment.articles_count}
Bullish: {sentiment.bullish_count}, Bearish: {sentiment.bearish_count}, Neutral: {sentiment.neutral_count}
Average Score: {sentiment.average_score:+.2f} ({sentiment.overall_sentiment})
Trend: {sentiment.sentiment_trend.capitalize()}
"""

        if sentiment.top_rationales:
            prompt += "\nTop News Rationales:\n"
            for i, rationale in enumerate(sentiment.top_rationales, 1):
                prompt += f"{i}. \"{rationale}\"\n"
        
        prompt += "\nPredict: UP, DOWN, or NEUTRAL with confidence (0-1) and reasoning."
        
        return prompt
    
    async def predict(
        self,
        symbol: str,
        candles: List[Dict],
        sentiments: List[Dict]
    ) -> PricePredictionResponse:
        """
        Generate price prediction using AI.
        
        Args:
            symbol: Trading symbol (e.g., "BTCUSDT")
            candles: Historical price candles (100+ recommended)
            sentiments: Recent sentiment data (24h)
        
        Returns:
            PricePredictionResponse with prediction, confidence, and reasoning
        """
        logger.info(f"Generating prediction for {symbol} with {len(candles)} candles and {len(sentiments)} sentiment entries")
        
        # Step 1: Calculate technical indicators
        technical = self.calculate_technical_indicators(candles)
        logger.info(f"Technical indicators calculated: RSI={technical.rsi}, MACD={technical.macd_signal}")
        
        # Step 2: Aggregate sentiment
        sentiment = self.aggregate_sentiment(sentiments)
        logger.info(f"Sentiment aggregated: {sentiment.overall_sentiment} ({sentiment.average_score})")
        
        # Step 3: Build prompt
        user_prompt = self.build_prediction_prompt(symbol, technical, sentiment)
        
        # Step 4: Call Gemini AI
        try:
            logger.info(f"Calling Gemini AI with model: {self.primary_model}")
            response = self.client.models.generate_content(
                model=self.primary_model,
                contents=user_prompt,
                config=types.GenerateContentConfig(
                    system_instruction=self.SYSTEM_PROMPT,
                    temperature=0.3,
                    max_output_tokens=800,
                    response_mime_type="application/json"
                )
            )
            result_text = response.text
            logger.info("Received response from Gemini AI")
            
        except Exception as e:
            logger.warning(f"Primary model failed: {e}. Trying fallback model...")
            try:
                response = self.client.models.generate_content(
                    model=self.fallback_model,
                    contents=user_prompt,
                    config=types.GenerateContentConfig(
                        system_instruction=self.SYSTEM_PROMPT,
                        temperature=0.3,
                        max_output_tokens=800,
                        response_mime_type="application/json"
                    )
                )
                result_text = response.text
                logger.info("Fallback model succeeded")
            except Exception as fallback_error:
                logger.error(f"Both models failed: {fallback_error}")
                raise ValueError("AI prediction failed with both primary and fallback models")
        
        # Step 5: Parse response
        import json
        try:
            result = json.loads(result_text)
            
            return PricePredictionResponse(
                symbol=symbol,
                prediction=result['prediction'],
                confidence=max(0.0, min(1.0, float(result['confidence']))),
                reasoning=result['reasoning'],
                key_factors=result.get('key_factors', []),
                risk_factors=result.get('risk_factors', []),
                technical_indicators=technical,
                sentiment_summary=sentiment,
                predicted_at=datetime.utcnow()
            )
        
        except (json.JSONDecodeError, KeyError, ValueError) as e:
            logger.error(f"Failed to parse AI response: {e}")
            logger.debug(f"Raw response: {result_text}")
            raise ValueError(f"Invalid AI response format: {e}")
