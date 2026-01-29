from fastapi import FastAPI, HTTPException, Depends
from contextlib import asynccontextmanager
import logging

from models import NewsDetail, SymbolSentiment
from services import SentimentAnalysisService
from config import get_settings

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)

sentiment_service: SentimentAnalysisService | None = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan handler for startup/shutdown."""
    global sentiment_service
    
    settings = get_settings()
    if not settings.GEMINI_API_KEY:
        logger.warning("GEMINI_API_KEY not set. Sentiment analysis will not work.")
    else:
        sentiment_service = SentimentAnalysisService()
        logger.info(f"Sentiment Analysis Service initialized with model: {settings.GEMINI_MODEL}")
    
    yield
    
    logger.info("Shutting down Analysis Service")


app = FastAPI(
    title="Analysis Service",
    description="AI-powered financial news sentiment analysis service",
    version="1.0.0",
    lifespan=lifespan
)


@app.post("/api/sentiment/analyze", response_model=list[SymbolSentiment])
def analyze_sentiment(
    news: NewsDetail,
    service: SentimentAnalysisService = Depends(sentiment_service)
):
    """
    Analyze a news article for financial sentiment.
    
    Extracts all mentioned financial symbols and provides:
    - Symbol: Ticker symbol of the financial instrument
    - Category: Bullish, Bearish, or Neutral
    - Score: -1.0 (strong sell) to +1.0 (strong buy)
    - Rationale: Brief explanation of expected price impact
    """
    try:
        logger.info('Start analysing sentiment for news article.')
        result = service.analyze(news)
        logger.info('Finish analysing sentiment for news article.')
        return result
    except ValueError as e:
        raise HTTPException(status_code=422, detail=str(e))
    except Exception as e:
        logger.error(f"Analysis failed: {e}")
        raise HTTPException(status_code=500, detail="Analysis failed. Please try again.")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
