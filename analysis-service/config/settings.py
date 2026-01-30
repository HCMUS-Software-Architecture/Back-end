from pydantic_settings import BaseSettings, SettingsConfigDict
from functools import lru_cache
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent

class Settings(BaseSettings):
    """Application settings loaded from environment variables."""
    
    # Gemini API (for sentiment analysis)
    GEMINI_API_KEY: str = ""
    GEMINI_MODEL: str = "gemini-2.0-flash-exp"
    
    # OpenRouter AI (for price prediction)
    OPENROUTER_API_KEY: str = "sk-or-v1-free"  # Free tier default
    OPENROUTER_BASE_URL: str = "https://openrouter.ai/api/v1"
    PRIMARY_MODEL: str = "meta-llama/llama-3.2-3b-instruct:free"
    FALLBACK_MODEL: str = "google/gemini-2.0-flash-exp:free"

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
