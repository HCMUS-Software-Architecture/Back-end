import asyncio
from fastapi import FastAPI, HTTPException, Depends
from contextlib import asynccontextmanager
import logging

from models import NewsDetail, SymbolSentiment, PricePredictionRequest, PricePredictionResponse
from services import SentimentAnalysisService, PricePredictorService
from config import get_settings
from database import MongoDB
from repositories import NewsRepository, SentimentRepository, PriceRepository
from messaging import get_consumer, RabbitMQConsumer

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)

sentiment_service: SentimentAnalysisService | None = None
predictor_service: PricePredictorService | None = None


async def process_news_message(news: NewsDetail) -> None:
    """Process a news article received from the message queue."""
    global sentiment_service
    
    if not sentiment_service:
        logger.error("Sentiment service not initialized, cannot process message")
        return
    
    try:
        logger.info(f'Processing news article from queue: "{news.header}"')
        
        logger.info('Saving news article to database.')
        news_id = await NewsRepository.insert(news)
        logger.info(f'Saved news article with ID: {news_id}')
        
        logger.info('Performing sentiment analysis.')
        sentiments = sentiment_service.analyze(news)
        logger.info(f'Completed sentiment analysis, found {len(sentiments)} symbols.')
        
        if sentiments:
            logger.info('Saving sentiment results to database.')
            sentiment_ids = await SentimentRepository.insert_many(
                sentiments=sentiments,
                news_id=news_id
            )
            logger.info(f'Saved {len(sentiment_ids)} sentiment results.')
        
        logger.info(f'Successfully processed news article: "{news.header}"')
        
    except Exception as e:
        logger.error(f"Failed to process news article: {e}")
        raise


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan handler for startup/shutdown."""
    global sentiment_service, predictor_service
    
    await MongoDB.connect()
    
    settings = get_settings()
    
    # Initialize sentiment analysis service
    if not settings.GEMINI_API_KEY:
        logger.warning("GEMINI_API_KEY not set. Sentiment analysis will not work.")
    else:
        sentiment_service = SentimentAnalysisService()
        logger.info(f"Sentiment Analysis Service initialized with model: {settings.GEMINI_MODEL}")
    
    # Initialize price predictor service
    predictor_service = PricePredictorService()
    logger.info(f"Price Predictor Service initialized with model: {settings.PRIMARY_MODEL}")
    
    consumer = get_consumer()
    consumer.set_message_handler(process_news_message)
    
    try:
        await consumer.connect()
        asyncio.create_task(consumer.start_consuming())
        logger.info("RabbitMQ consumer started")
    except Exception as e:
        logger.warning(f"Failed to start RabbitMQ consumer: {e}")
    
    yield
    
    await consumer.disconnect()
    await MongoDB.disconnect()
    logger.info("Shutting down Analysis Service")


app = FastAPI(
    title="Analysis Service",
    description="AI-powered financial news sentiment analysis service",
    version="1.0.0",
    lifespan=lifespan
)


# Health check endpoint for Kubernetes probes
@app.get("/health")
async def health_check():
    """Health check endpoint for Kubernetes liveness and readiness probes."""
    return {
        "status": "healthy",
        "services": {
            "mongodb": "connected" if MongoDB.database is not None else "disconnected",
            "sentiment_service": "ready" if sentiment_service else "not_initialized",
            "predictor_service": "ready" if predictor_service else "not_initialized"
        }
    }


@app.post("/api/sentiment/analyze", response_model=list[SymbolSentiment])
async def analyze_sentiment(
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
        logger.info('Start saving news article to database.')
        news_id = await NewsRepository.insert(news)
        logger.info(f'Finish saving news article with ID: {news_id}')

        logger.info('Start analysing sentiment for news article.')
        sentiments = service.analyze(news)
        logger.info('Finish analysing sentiment for news article.')

        logger.info('Start saving sentiment analysis results to database.')
        sentiment_ids = await SentimentRepository.insert_many(
            sentiments=sentiments,
            news_id=news_id
        )
        logger.info(f'Finish saving {len(sentiment_ids)} sentiment results to database.')

        return sentiments
    except ValueError as e:
        raise HTTPException(status_code=422, detail=str(e))
    except Exception as e:
        logger.error(f"Analysis failed: {e}")
        raise HTTPException(status_code=500, detail="Analysis failed. Please try again.")


@app.post("/api/predict/price", response_model=PricePredictionResponse)
async def predict_price(
    request: PricePredictionRequest
):
    """
    Predict price direction (UP/DOWN/NEUTRAL) using AI analysis.
    
    Combines:
    - Historical price data (last 100 candles)
    - Technical indicators (RSI, MACD, MA)
    - Recent news sentiment (24h window)
    
    Returns:
    - Prediction: UP, DOWN, or NEUTRAL
    - Confidence: 0.0 - 1.0
    - Reasoning: AI-generated explanation
    - Key factors and risk factors
    """
    global predictor_service
    
    if not predictor_service:
        raise HTTPException(
            status_code=503,
            detail="Price predictor service not initialized"
        )
    
    try:
        logger.info(f"Received prediction request for {request.symbol} ({request.interval})")
        
        # Fetch historical price data
        logger.info("Fetching historical price candles from MongoDB")
        candles = await PriceRepository.get_historical_candles(
            symbol=request.symbol,
            interval=request.interval,
            limit=100
        )
        
        if len(candles) < 50:
            raise HTTPException(
                status_code=400,
                detail=f"Insufficient price data. Need at least 50 candles, got {len(candles)}"
            )
        
        logger.info(f"Fetched {len(candles)} candles")
        
        # Fetch recent sentiment data
        logger.info("Fetching recent sentiment data from MongoDB")
        sentiments = await SentimentRepository.get_recent_sentiments(
            symbol=request.symbol,
            hours=24
        )
        
        logger.info(f"Fetched {len(sentiments)} sentiment entries")
        
        # Generate prediction
        logger.info("Generating AI prediction")
        prediction = await predictor_service.predict(
            symbol=request.symbol,
            candles=candles,
            sentiments=sentiments
        )
        
        logger.info(f"Prediction complete: {prediction.prediction} (confidence: {prediction.confidence:.2f})")
        
        return prediction
        
    except HTTPException:
        raise
    except ValueError as e:
        logger.error(f"Validation error: {e}")
        raise HTTPException(status_code=422, detail=str(e))
    except Exception as e:
        logger.error(f"Prediction failed: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Prediction failed: {str(e)}")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
