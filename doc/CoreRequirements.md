**Prerequisite:** Select a currency pair (e.g., BTCUSDT, etc.)

1. **Financial News Collection (from multiple sources) – Crawler**

   - Identify relevant information necessary for data analysis.
   - Each website has a different HTML structure; implement automated learning of each site's structure to enable automatic information extraction. Consider cases where websites change their HTML structure.
   - Store data comprehensively and display selectively on the GUI.

2. **Price Chart Display (similar to [TradingView](https://vn.tradingview.com/chart/)), Binance Exchange**

   - Fetch historical price data via APIs from exchanges.
   - Display real-time prices using WebSocket for each exchange.
   - Support multiple timeframes and currency pairs.
   - Apply scalable architecture to meet multi-user requirements.

3. **AI Models for News Analysis**

   - Align news data with historical price data for AI model input.
   - Utilize available (black-box) AI models.
   - Perform causal analysis (advanced):
     - Example: Predict next hour/day trend (UP/DOWN) and provide reasoning.

4. **Account Management**

**Suggestion:** Sentiment Analysis
