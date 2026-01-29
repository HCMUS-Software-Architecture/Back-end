from pydantic import BaseModel, Field
from typing import List, Literal
from datetime import datetime


class NewsDetail(BaseModel):
    header: str
    subheader: str
    content: str
    url: str
    crawled_at: datetime


class SymbolSentiment(BaseModel):
    symbol: str
    category: Literal["Bullish", "Bearish", "Neutral"]
    score: float = Field(ge=-1.0, le=1.0)
    rationale: str


class SentimentAnalysisResponse(BaseModel):
    symbols: List[SymbolSentiment]

