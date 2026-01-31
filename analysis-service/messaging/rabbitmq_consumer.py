import asyncio
import json
import logging
from datetime import datetime
from typing import Callable, Awaitable

import aio_pika
from aio_pika import Message, ExchangeType
from aio_pika.abc import AbstractIncomingMessage

from config.settings import get_settings
from models.schemas import NewsDetail

logger = logging.getLogger(__name__)

NEWS_ANALYSIS_QUEUE = "news_analysis_queue"
NEWS_ANALYSIS_EXCHANGE = "news_analysis_exchange"


class RabbitMQConsumer:
    """RabbitMQ consumer for receiving news articles for sentiment analysis."""
    
    def __init__(self):
        self.settings = get_settings()
        self.connection = None
        self.channel = None
        self.queue = None
        self._message_handler: Callable[[NewsDetail], Awaitable[None]] | None = None
    
    async def connect(self) -> None:
        """Connect to RabbitMQ and set up the queue."""
        rabbitmq_url = self.settings.RABBITMQ_URL
        
        try:
            logger.info(f"Connecting to RabbitMQ at {rabbitmq_url}")
            self.connection = await aio_pika.connect_robust(rabbitmq_url)
            self.channel = await self.connection.channel()
            
            await self.channel.set_qos(prefetch_count=1)
            
            exchange = await self.channel.declare_exchange(
                NEWS_ANALYSIS_EXCHANGE,
                ExchangeType.DIRECT,
                durable=True
            )
            
            self.queue = await self.channel.declare_queue(
                NEWS_ANALYSIS_QUEUE,
                durable=True
            )
            
            await self.queue.bind(exchange, routing_key=NEWS_ANALYSIS_QUEUE)
            
            logger.info("Successfully connected to RabbitMQ")
        except Exception as e:
            logger.error(f"Failed to connect to RabbitMQ: {e}")
            raise
    
    async def disconnect(self) -> None:
        """Disconnect from RabbitMQ."""
        try:
            if self.connection:
                await self.connection.close()
            logger.info("Disconnected from RabbitMQ")
        except Exception as e:
            logger.error(f"Error disconnecting from RabbitMQ: {e}")
    
    def set_message_handler(
        self,
        handler: Callable[[NewsDetail], Awaitable[None]]
    ) -> None:
        """Set the handler function for processing incoming messages."""
        self._message_handler = handler
    
    async def _process_message(self, message: AbstractIncomingMessage) -> None:
        """Process an incoming message from the queue."""
        async with message.process():
            try:
                body = message.body.decode()
                data = json.loads(body)
                
                logger.info(f"Received news article: {data.get('header', 'Unknown')}")
                
                news = NewsDetail(
                    header=data["header"],
                    subheader=data["subheader"],
                    content=data["content"],
                    url=data["url"],
                    crawled_at=datetime.fromisoformat(
                        data["crawled_at"].replace("Z", "+00:00")
                    )
                )
                
                if self._message_handler:
                    await self._message_handler(news)
                else:
                    logger.warning("No message handler set, message will be discarded")
                    
            except json.JSONDecodeError as e:
                logger.error(f"Failed to parse message as JSON: {e}")
            except KeyError as e:
                logger.error(f"Missing required field in message: {e}")
            except Exception as e:
                logger.error(f"Error processing message: {e}")
    
    async def start_consuming(self) -> None:
        """Start consuming messages from the queue."""
        if not self.queue:
            raise RuntimeError("Not connected to RabbitMQ. Call connect() first.")
        
        logger.info(f"Starting to consume messages from queue: {NEWS_ANALYSIS_QUEUE}")
        await self.queue.consume(self._process_message)
    
    async def run_forever(self) -> None:
        """Run the consumer indefinitely."""
        await self.connect()
        await self.start_consuming()
        
        try:
            await asyncio.Future()
        except asyncio.CancelledError:
            pass
        finally:
            await self.disconnect()


_consumer: RabbitMQConsumer | None = None


def get_consumer() -> RabbitMQConsumer:
    """Get the RabbitMQ consumer singleton."""
    global _consumer
    if _consumer is None:
        _consumer = RabbitMQConsumer()
    return _consumer
