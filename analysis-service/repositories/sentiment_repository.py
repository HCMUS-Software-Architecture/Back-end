from datetime import datetime
from typing import List

from database import get_sentiment_collection
from models.schemas import (
    SymbolSentiment
)

class SentimentRepository:    

    @staticmethod
    async def insert_many(
        sentiments: List[SymbolSentiment],
        news_id: str
    ) -> List[str]:
        if not sentiments:
            return []
        
        collection = get_sentiment_collection()
        
        docs = [
            {
                **s.model_dump(),
                "news_id": news_id,
                "analyzed_at": datetime.utcnow()
            }
            for s in sentiments
        ]
        
        result = await collection.insert_many(docs)
        sentiment_ids = [str(id) for id in result.inserted_ids]
        
        return sentiment_ids
