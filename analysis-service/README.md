# Analysis Service

AI-powered financial news sentiment analysis service using Google Gemini.

## Features

- **Sentiment Analysis**: Analyze news articles for market impact on financial symbols
- **Symbol Extraction**: Automatically identify mentioned stocks, crypto, ETFs, and indices
- **Market Impact Scoring**: Score from -1.0 (strong sell) to +1.0 (strong buy)
- **Rationale Generation**: Brief explanation of expected price impact

## Prerequisites

- Python 3.11+
- Google Gemini API Key ([Get one here](https://aistudio.google.com/apikey))

## Project Structure

```
analysis-service/
├── main.py                 # FastAPI application entry point
├── requirements.txt        # Python dependencies
├── .env                    # Environment variables (create from .env.example)
├── .env.example            # Environment template
├── config/
│   ├── __init__.py
│   ├── settings.py         # Configuration management
│   └── prompts.py          # AI prompt templates
├── models/
│   ├── __init__.py
│   └── schemas.py          # Pydantic data models
└── services/
    ├── __init__.py
    └── sentiment_analysis_service.py  # Core analysis service
```

## Setup

### 1. Create Virtual Environment

```bash
cd analysis-service

# Create virtual environment
python -m venv .venv

# Activate virtual environment
# Windows
.venv\Scripts\activate

# Linux/macOS
source .venv/bin/activate
```

### 2. Install Dependencies

```bash
pip install -r requirements.txt
```

### 3. Configure Environment

```bash
# Copy example environment file
cp .env.example .env

# Edit .env and add your Gemini API key
```

**.env file content:**
```dotenv
GEMINI_API_KEY=your_gemini_api_key_here
GEMINI_MODEL=gemini-2.0-flash
DEBUG=false
```

## Running the Service

### Development Mode (with auto-reload)

```bash
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

### Production Mode

```bash
uvicorn main:app --host 0.0.0.0 --port 8000 --workers 4
```

### Using Python directly

```bash
python main.py
```

## API Documentation

Once the service is running, access the interactive API docs:

- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc

## API Endpoints

### POST `/api/sentiment/analyze`

Analyze a news article for financial sentiment.

**Request Body:**
```json
{
  "title": "SEC Approves Spot Bitcoin ETF",
  "summary": "Historic decision opens crypto to institutional investors",
  "content": "The Securities and Exchange Commission has approved the first spot Bitcoin ETF, marking a historic moment for the cryptocurrency industry..."
}
```

**Response:**
```json
[
  {
    "symbol": "BTC",
    "category": "Bullish",
    "score": 0.85,
    "rationale": "ETF approval signals institutional adoption and regulatory clarity, likely driving significant buy pressure."
  },
  {
    "symbol": "COIN",
    "category": "Bullish",
    "score": 0.6,
    "rationale": "Coinbase benefits from increased crypto trading activity and potential ETF custody services."
  }
]
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `symbol` | string | Ticker symbol (e.g., BTC, AAPL, ETH) |
| `category` | string | `Bullish`, `Bearish`, or `Neutral` |
| `score` | float | Market impact score from -1.0 to +1.0 |
| `rationale` | string | Brief explanation of expected price movement |

### Score Interpretation

| Score Range | Interpretation |
|-------------|----------------|
| +0.7 to +1.0 | Strong buy pressure |
| +0.3 to +0.7 | Moderate bullish |
| -0.3 to +0.3 | Neutral / No material impact |
| -0.7 to -0.3 | Moderate bearish |
| -1.0 to -0.7 | Strong sell pressure |

## Example Usage

### Using cURL

```bash
curl -X POST "http://localhost:8000/api/sentiment/analyze" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Tesla Reports Record Q4 Deliveries",
    "summary": "Electric vehicle maker exceeds analyst expectations",
    "content": "Tesla Inc. announced record fourth-quarter deliveries of 484,507 vehicles, surpassing Wall Street expectations of 475,000 units..."
  }'
```

### Using Python

```python
import requests

response = requests.post(
    "http://localhost:8000/api/sentiment/analyze",
    json={
        "title": "Tesla Reports Record Q4 Deliveries",
        "summary": "Electric vehicle maker exceeds analyst expectations",
        "content": "Tesla Inc. announced record fourth-quarter deliveries..."
    }
)

results = response.json()
for symbol in results:
    print(f"{symbol['symbol']}: {symbol['category']} ({symbol['score']})")
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `GEMINI_API_KEY` | Google Gemini API key (required) | - |
| `GEMINI_MODEL` | Gemini model to use | `gemini-2.0-flash` |
| `DEBUG` | Enable debug mode | `false` |

## Troubleshooting

### API Key Not Loading

1. Ensure `.env` file exists in `analysis-service/` directory
2. Check that `GEMINI_API_KEY` has no quotes or spaces
3. Restart the service after modifying `.env`

### 503 Service Unavailable

The sentiment service is not initialized. Check:
1. `GEMINI_API_KEY` is set correctly
2. Check logs for initialization errors

### 422 Validation Error

The request body is malformed. Ensure all required fields (`title`, `summary`, `content`) are provided.

## License

MIT
