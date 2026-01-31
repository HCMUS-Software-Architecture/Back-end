from datetime import datetime
import logging

from database import get_news_collection
from models.schemas import (
    NewsDetail
)

logger = logging.getLogger(__name__)


class NewsRepository:
    
    @staticmethod
    async def insert(news: NewsDetail) -> str:
        collection = get_news_collection()
        
        doc = {
            **news.model_dump(),
            "created_at": datetime.utcnow()
        }
        
        result = await collection.insert_one(doc)
        news_id = str(result.inserted_id)
        
        logger.info(f"Inserted news article: {news_id}")
        return news_id