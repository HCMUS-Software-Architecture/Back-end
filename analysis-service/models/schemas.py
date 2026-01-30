from pydantic import BaseModel, Field
from typing import List, Literal
from datetime import datetime


class NewsDetail(BaseModel):
    header: str
    subheader: str
    content: str
    url: str
    crawled_at: datetime


class NewsDetailInDB(NewsDetail):
    id: str = Field(alias="_id")
    created_at: datetime = Field(default_factory=datetime.utcnow)
    
    class Config:
        populate_by_name = True


class SymbolSentiment(BaseModel):
    symbol: str
    category: Literal["Bullish", "Bearish", "Neutral"]
    score: float = Field(ge=-1.0, le=1.0)
    rationale: str


class SymbolSentimentInDB(SymbolSentiment):
    id: str = Field(alias="_id")
    news_id: str
    analyzed_at: datetime = Field(default_factory=datetime.utcnow)
    
    class Config:
        populate_by_name = True


class SentimentAnalysisResponse(BaseModel):
    symbols: List[SymbolSentiment]


# ===== Price Prediction Models =====

class TechnicalIndicators(BaseModel):
    """Technical analysis indicators calculated from price data."""
    current_price: float
    price_change_24h: float
    high_24h: float
    low_24h: float
    rsi: float
    rsi_interpretation: Literal["oversold", "neutral", "overbought"]
    macd_signal: Literal["bullish", "bearish"]
    macd_value: float
    ma20: float
    ma50: float
    price_vs_ma20: Literal["above", "below"]
    price_vs_ma50: Literal["above", "below"]
    volume_change_pct: float
    volume_trend: Literal["high", "normal", "low"]


class SentimentSummary(BaseModel):
    """Aggregated sentiment data from recent news."""
    articles_count: int
    average_score: float
    bullish_count: int
    bearish_count: int
    neutral_count: int
    overall_sentiment: Literal["BULLISH", "BEARISH", "NEUTRAL"]
    sentiment_trend: Literal["improving", "stable", "declining"]
    top_rationales: List[str]


class PricePredictionRequest(BaseModel):
    """Request for price prediction."""
    symbol: str = Field(..., description="Trading symbol (e.g., BTCUSDT)")
    interval: str = Field(default="1h", description="Candle interval (1h, 4h, 1d)")


class PricePredictionResponse(BaseModel):
    """AI-generated price prediction response."""
    symbol: str
    prediction: Literal["UP", "DOWN", "NEUTRAL"]
    confidence: float = Field(..., ge=0.0, le=1.0, description="Confidence score 0-1")
    reasoning: str
    key_factors: List[str]
    risk_factors: List[str]
    technical_indicators: TechnicalIndicators
    sentiment_summary: SentimentSummary
    predicted_at: datetime = Field(default_factory=datetime.utcnow)
