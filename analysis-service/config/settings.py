from pydantic_settings import BaseSettings, SettingsConfigDict
from functools import lru_cache
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent

class Settings(BaseSettings):
    """Application settings loaded from environment variables."""
    
    # Gemini API (for both sentiment analysis and price prediction)
    GEMINI_API_KEY: str = ""
    GEMINI_MODEL: str = "gemini-2.0-flash"
    GEMINI_FALLBACK_MODEL: str = "gemini-1.5-flash"

    MONGODB_URI: str = ""
    MONGODB_DATABASE: str = "analysis_service"

    RABBITMQ_URL: str = ""

    APP_NAME: str = "Analysis Service"
    DEBUG: bool = False
    
    model_config = SettingsConfigDict(
        env_file=BASE_DIR / ".env",
        env_file_encoding="utf-8",
        extra="ignore"
    )


@lru_cache()
def get_settings() -> Settings:
    """Cached settings instance."""
    return Settings()
