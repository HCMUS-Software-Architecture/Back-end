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

