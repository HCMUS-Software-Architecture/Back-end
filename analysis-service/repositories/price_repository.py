"""
Repository for fetching historical price data from MongoDB.
"""

from datetime import datetime
from typing import List, Dict
from database import get_database


class PriceRepository:
    """Handle price data queries from MongoDB."""
    
    @staticmethod
    async def get_historical_candles(
        symbol: str,
        interval: str = "1h",
        limit: int = 100
    ) -> List[Dict]:
        """
        Fetch historical price candles from MongoDB.
        
        Args:
            symbol: Trading symbol (e.g., "BTCUSDT")
            interval: Candle interval (e.g., "1h", "4h", "1d")
            limit: Number of candles to fetch (default 100)
        
        Returns:
            List of candle dicts with OHLCV data, sorted oldest to newest
        """
        db = get_database()
        collection = db.get_collection("price_candles")
        
        # Query MongoDB for candles
        cursor = collection.find(
            {"symbol": symbol.upper(), "interval": interval},
            {
                "_id": 0,
                "openTime": 1,
                "open": 1,
                "high": 1,
                "low": 1,
                "close": 1,
                "volume": 1
            }
        ).sort("openTime", -1).limit(limit)
        
        candles = await cursor.to_list(length=limit)
        
        # Reverse to get oldest first (required for technical indicators)
        candles.reverse()
        
        return candles
