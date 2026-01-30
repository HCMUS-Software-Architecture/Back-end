from motor.motor_asyncio import AsyncIOMotorClient, AsyncIOMotorDatabase
from typing import Optional
import logging

from config import get_settings

logger = logging.getLogger(__name__)


class MongoDB:    
    client: Optional[AsyncIOMotorClient] = None
    database: Optional[AsyncIOMotorDatabase] = None
    
    @classmethod
    async def connect(cls) -> None:
        settings = get_settings()
        
        try:
            cls.client = AsyncIOMotorClient(settings.MONGODB_URI)
            cls.database = cls.client[settings.MONGODB_DATABASE]
            
            await cls.client.admin.command('ping')
            logger.info(f"Connected to MongoDB: {settings.MONGODB_DATABASE}")
            
        except Exception as e:
            logger.error(f"Failed to connect to MongoDB: {e}")
            raise
    
    @classmethod
    async def disconnect(cls) -> None:
        if cls.client:
            cls.client.close()
            logger.info("Disconnected from MongoDB")
    
    @classmethod
    def get_database(cls) -> AsyncIOMotorDatabase:
        """Get database instance."""
        if cls.database is None:
            raise RuntimeError("MongoDB not connected. Call connect() first.")
        return cls.database
    
    @classmethod
    def get_collection(cls, name: str):
        return cls.get_database()[name]


NEWS_COLLECTION = "news_details"
SENTIMENT_COLLECTION = "symbol_sentiments"


def get_news_collection():
    return MongoDB.get_collection(NEWS_COLLECTION)


def get_sentiment_collection():
    return MongoDB.get_collection(SENTIMENT_COLLECTION)


def get_database():
    """Get MongoDB database instance."""
    return MongoDB.get_database()
