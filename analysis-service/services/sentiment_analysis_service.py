import json
import logging
from datetime import datetime
from typing import List

from google import genai
from google.genai import types

from models.schemas import (
    NewsDetail,
    SymbolSentiment,
    SentimentAnalysisResponse
)
from config.prompts import SENTIMENT_ANALYSIS_SYSTEM_INSTRUCTION
from config.settings import get_settings

logger = logging.getLogger(__name__)


class SentimentAnalysisService:

    def __init__(self):
        settings = get_settings()
        self.api_key = settings.GEMINI_API_KEY
        self.model_name = settings.GEMINI_MODEL
        self.client = genai.Client(api_key=self.api_key)

    def _build_news_text(self, news: NewsDetail) -> str:
        return f"""
            - Header: {news.header}
            - Subheader: {news.subheader}
            - Content: {news.content}
        """

    def _parse_response(self, response_text: str) -> List[SymbolSentiment]:
        try:
            cleaned_text = response_text.strip()
            if cleaned_text.startswith("```json"):
                cleaned_text = cleaned_text[7:]
            if cleaned_text.startswith("```"):
                cleaned_text = cleaned_text[3:]
            if cleaned_text.endswith("```"):
                cleaned_text = cleaned_text[:-3]
            cleaned_text = cleaned_text.strip()
            
            data = json.loads(cleaned_text)
            
            response = SentimentAnalysisResponse(**data)
            return response.symbols
            
        except json.JSONDecodeError as e:
            logger.error(f"Failed to parse JSON response: {e}")
            logger.debug(f"Raw response: {response_text}")
            raise ValueError(f"Invalid JSON response from model: {e}")
        except Exception as e:
            logger.error(f"Failed to parse response: {e}")
            raise ValueError(f"Failed to parse model response: {e}")

    def analyze(self, news: NewsDetail) -> List[SymbolSentiment]:
        news_text = self._build_news_text(news)
        
        try:
            logger.info('Start sending request to Gemini model for sentiment analysis.')
            response = self.client.models.generate_content(
                model=self.model_name,
                contents=f'News article:\n"""\n{news_text}\n"""',
                config=types.GenerateContentConfig(
                    system_instruction=SENTIMENT_ANALYSIS_SYSTEM_INSTRUCTION,
                    temperature=0.5,
                    response_mime_type="application/json",
                )
            )
            logger.info('Finish receiving response from Gemini model for sentiment analysis.')
            
            logger.info('Start parsing response from Gemini model.')
            symbols = self._parse_response(response.text)
            logger.info('Finish parsing response from Gemini model.')
            
            return symbols
            
        except Exception as e:
            logger.error(f"Sentiment analysis failed: {e}")
            raise
