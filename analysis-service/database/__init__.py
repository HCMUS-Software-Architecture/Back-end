# Database package
from .mongodb import (
    MongoDB,
    NEWS_COLLECTION,
    SENTIMENT_COLLECTION,
    get_news_collection,
    get_sentiment_collection
)

__all__ = [
    "MongoDB",
    "NEWS_COLLECTION",
    "SENTIMENT_COLLECTION",
    "get_news_collection",
    "get_sentiment_collection"
]
