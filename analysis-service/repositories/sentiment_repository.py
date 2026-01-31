from datetime import datetime, timedelta
from typing import List, Dict

from database import get_sentiment_collection, get_database
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
    
    @staticmethod
    async def get_recent_sentiments(
        symbol: str,
        hours: int = 24
    ) -> List[Dict]:
        """
        Fetch recent sentiment data for a symbol.
        
        Args:
            symbol: Trading symbol (e.g., "BTCUSDT")
            hours: Look back period in hours (default 24)
        
        Returns:
            List of sentiment dicts sorted by analyzed_at (newest first)
        """
        db = get_database()
        collection = db.get_collection("sentiments")
        
        cutoff_time = datetime.utcnow() - timedelta(hours=hours)
        
        cursor = collection.find(
            {
                "symbol": symbol.upper(),
                "analyzed_at": {"$gte": cutoff_time}
            },
            {
                "_id": 0,
                "category": 1,
                "score": 1,
                "rationale": 1,
                "analyzed_at": 1
            }
        ).sort("analyzed_at", -1)
        
        sentiments = await cursor.to_list(length=None)
        
        return sentiments
