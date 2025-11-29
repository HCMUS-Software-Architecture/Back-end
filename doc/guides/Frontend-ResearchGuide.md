# Frontend Research & Implementation Guide

**For Trading Platform - Real-time Financial Data Display**

---

## Table of Contents

1. [Overview](#overview)
2. [Technology Stack Selection](#technology-stack-selection)
3. [TradingView Chart Libraries](#tradingview-chart-libraries)
4. [Real-time Data Libraries](#real-time-data-libraries)
5. [UI Component Libraries](#ui-component-libraries)
6. [State Management](#state-management)
7. [Reference Projects & Repositories](#reference-projects--repositories)
8. [Implementation Approach](#implementation-approach)
9. [Adaptation Strategy](#adaptation-strategy)
10. [PowerShell Commands](#powershell-commands)
11. [References](#references)

---

## Overview

### Goals

- Build a responsive, real-time trading dashboard
- Display TradingView-style interactive charts
- Stream live price data via WebSocket
- Aggregate and display financial news with sentiment
- Ensure performant rendering with large datasets

### Core Requirements

Based on [CoreRequirements.md](../CoreRequirements.md):

1. **Price Chart Display** - TradingView-style charts with real-time updates
2. **Multiple Timeframes** - 1m, 5m, 15m, 1h, 4h, 1d
3. **News Display** - Articles with sentiment indicators
4. **Currency Pair Selection** - BTCUSDT, ETHUSDT, etc.
5. **WebSocket Updates** - Sub-second price updates

---

## Technology Stack Selection

### Recommended Stack

| Technology | Version | Purpose | Rationale |
|------------|---------|---------|-----------|
| **Next.js** | 14+ | Framework | SSR, API routes, excellent DX |
| **TypeScript** | 5+ | Language | Type safety, better tooling |
| **TailwindCSS** | 3+ | Styling | Rapid development, responsive |
| **lightweight-charts** | 4+ | Charts | Official TradingView library |
| **TanStack Query** | 5+ | Data fetching | Caching, auto-refresh |
| **Zustand** | 4+ | State management | Simple, performant |
| **Socket.io-client** | 4+ | WebSocket | Robust reconnection |

### Alternative Options

| Category | Alternative | Use Case |
|----------|-------------|----------|
| Charts | Apache ECharts | If need more chart types |
| Charts | Highcharts | Enterprise license available |
| State | Redux Toolkit | Complex state requirements |
| WebSocket | native WebSocket | Simpler requirements |
| Styling | shadcn/ui | Pre-built components |

---

## TradingView Chart Libraries

### 1. Lightweight Charts (Recommended)

**Repository**: [tradingview/lightweight-charts](https://github.com/tradingview/lightweight-charts)

**Stars**: 7k+ | **License**: Apache 2.0

**Pros**:
- Official TradingView library
- Small bundle size (~45KB)
- High performance
- Mobile friendly
- Free and open source

**Cons**:
- Fewer chart types than full TradingView
- Limited built-in indicators

**Installation**:

**Bash (Linux/macOS):**
```bash
npm install lightweight-charts
```

**PowerShell (Windows 10/11):**
```powershell
npm install lightweight-charts
```

**Basic Usage**:

```typescript
import { createChart, CandlestickSeries } from 'lightweight-charts';

const chart = createChart(container, {
  width: 800,
  height: 400,
  layout: {
    background: { color: '#1a1a2e' },
    textColor: '#d1d4dc',
  },
});

const candlestickSeries = chart.addCandlestickSeries({
  upColor: '#26a69a',
  downColor: '#ef5350',
});

candlestickSeries.setData([
  { time: '2023-01-01', open: 100, high: 105, low: 98, close: 103 },
  // ... more data
]);
```

### 2. React Financial Charts

**Repository**: [react-financial/react-financial-charts](https://github.com/react-financial/react-financial-charts)

**Stars**: 600+ | **License**: MIT

**Pros**:
- React-native integration
- Built-in indicators (SMA, EMA, RSI, MACD)
- Extensive customization

**Cons**:
- Larger bundle size
- More complex setup

**Installation**:

```bash
npm install @react-financial-charts/core @react-financial-charts/series
```

### 3. TradingView Charting Library (Commercial)

**Website**: [TradingView Charting Library](https://www.tradingview.com/charting-library-docs/)

**Pros**:
- Full TradingView experience
- 100+ built-in indicators
- Drawing tools

**Cons**:
- Commercial license required
- Larger bundle
- More complex integration

---

## Real-time Data Libraries

### 1. Socket.io-client (Recommended)

**Repository**: [socketio/socket.io-client](https://github.com/socketio/socket.io-client)

**Stars**: 10k+ | **License**: MIT

**Features**:
- Automatic reconnection
- Binary streaming
- Multiplexing
- Room support

**Installation**:

```bash
npm install socket.io-client
```

**Usage with React**:

```typescript
import { io, Socket } from 'socket.io-client';
import { useEffect, useState } from 'react';

export function usePriceSocket(symbol: string) {
  const [price, setPrice] = useState<number | null>(null);
  
  useEffect(() => {
    const socket = io('ws://localhost:8080', {
      path: '/ws/prices',
      transports: ['websocket'],
    });
    
    socket.on('connect', () => {
      socket.emit('subscribe', symbol);
    });
    
    socket.on('price', (data) => {
      setPrice(data.price);
    });
    
    return () => {
      socket.disconnect();
    };
  }, [symbol]);
  
  return price;
}
```

### 2. @stomp/stompjs

**Repository**: [stomp-js/stompjs](https://github.com/stomp-js/stompjs)

**Stars**: 400+ | **License**: Apache 2.0

**Best for**: Spring WebSocket integration

**Installation**:

```bash
npm install @stomp/stompjs sockjs-client
```

**Usage**:

```typescript
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const client = new Client({
  webSocketFactory: () => new SockJS('http://localhost:8080/ws/prices'),
  onConnect: () => {
    client.subscribe('/topic/prices/btcusdt', (message) => {
      const tick = JSON.parse(message.body);
      console.log('Price:', tick.price);
    });
  },
});

client.activate();
```

### 3. use-websocket Hook

**Repository**: [robtaussig/react-use-websocket](https://github.com/robtaussig/react-use-websocket)

**Stars**: 1.5k+ | **License**: MIT

**Best for**: Simple React integration

```typescript
import useWebSocket from 'react-use-websocket';

function PriceComponent({ symbol }) {
  const { lastMessage } = useWebSocket(`ws://localhost:8080/ws/prices/${symbol}`);
  
  const price = lastMessage ? JSON.parse(lastMessage.data).price : null;
  
  return <div>Price: {price}</div>;
}
```

---

## UI Component Libraries

### 1. shadcn/ui (Recommended)

**Repository**: [shadcn/ui](https://github.com/shadcn-ui/ui)

**Stars**: 60k+ | **License**: MIT

**Features**:
- Copy-paste components
- Built on Radix UI
- TailwindCSS styling
- Fully customizable

**Installation**:

```bash
npx shadcn-ui@latest init
npx shadcn-ui@latest add button card table
```

### 2. Radix UI

**Repository**: [radix-ui/primitives](https://github.com/radix-ui/primitives)

**Stars**: 14k+ | **License**: MIT

**Best for**: Accessible, unstyled primitives

### 3. Mantine

**Repository**: [mantinedev/mantine](https://github.com/mantinedev/mantine)

**Stars**: 24k+ | **License**: MIT

**Best for**: Complete component library with hooks

---

## State Management

### 1. Zustand (Recommended)

**Repository**: [pmndrs/zustand](https://github.com/pmndrs/zustand)

**Stars**: 42k+ | **License**: MIT

**Why Zustand**:
- Simple API
- Small bundle (1KB)
- TypeScript native
- Works with React DevTools

**Usage**:

```typescript
import { create } from 'zustand';

interface PriceState {
  prices: Record<string, number>;
  setPrice: (symbol: string, price: number) => void;
}

export const usePriceStore = create<PriceState>((set) => ({
  prices: {},
  setPrice: (symbol, price) =>
    set((state) => ({
      prices: { ...state.prices, [symbol]: price },
    })),
}));

// Usage in component
function PriceDisplay({ symbol }: { symbol: string }) {
  const price = usePriceStore((state) => state.prices[symbol]);
  return <div>{price}</div>;
}
```

### 2. TanStack Query (for server state)

**Repository**: [TanStack/query](https://github.com/TanStack/query)

**Stars**: 39k+ | **License**: MIT

**Usage**:

```typescript
import { useQuery } from '@tanstack/react-query';

function useArticles(page: number) {
  return useQuery({
    queryKey: ['articles', page],
    queryFn: () => fetch(`/api/articles?page=${page}`).then(r => r.json()),
    refetchInterval: 60000, // Refresh every minute
  });
}
```

---

## Reference Projects & Repositories

### 1. Complete Trading Platforms

| Repository | Stars | Description | License |
|------------|-------|-------------|---------|
| [Gekko](https://github.com/askmike/gekko) | 10k+ | Bitcoin trading bot with UI | MIT |
| [Freqtrade](https://github.com/freqtrade/freqtrade) | 25k+ | Crypto trading bot | GPL-3.0 |
| [Hummingbot](https://github.com/hummingbot/hummingbot) | 6k+ | Market making bot | Apache 2.0 |

### 2. Chart Examples

| Repository | Description | Key Features |
|------------|-------------|--------------|
| [lightweight-charts-react-example](https://github.com/nicholasbergesen/lightweight-charts-react-example) | React integration | Hooks, real-time updates |
| [trading-vue-js](https://github.com/tvjsx/trading-vue-js) | Vue.js charts | Customizable overlays |
| [react-stockcharts](https://github.com/rrag/react-stockcharts) | Stock charts | Technical indicators |

### 3. News Aggregation

| Repository | Description | Key Features |
|------------|-------------|--------------|
| [newsboat](https://github.com/newsboat/newsboat) | RSS reader | Feed parsing |
| [mercury-parser](https://github.com/postlight/mercury-parser) | Article extraction | Clean content |

### 4. Real-time Dashboards

| Repository | Description | Key Features |
|------------|-------------|--------------|
| [grafana](https://github.com/grafana/grafana) | Metrics dashboard | Real-time charts |
| [metabase](https://github.com/metabase/metabase) | BI tool | SQL-based dashboards |

---

## Implementation Approach

### Phase 1: Basic Setup

```bash
# Create Next.js app
npx create-next-app@latest trading-frontend --typescript --tailwind --eslint --app --src-dir

cd trading-frontend

# Add essential dependencies
npm install lightweight-charts @tanstack/react-query zustand socket.io-client
npm install -D @types/sockjs-client

# Add UI components
npx shadcn-ui@latest init
npx shadcn-ui@latest add button card table tabs
```

### Phase 2: Project Structure

```
trading-frontend/
├── src/
│   ├── app/
│   │   ├── page.tsx           # Dashboard
│   │   ├── chart/page.tsx     # Chart view
│   │   └── news/page.tsx      # News feed
│   ├── components/
│   │   ├── charts/
│   │   │   ├── PriceChart.tsx
│   │   │   └── ChartControls.tsx
│   │   ├── news/
│   │   │   ├── NewsFeed.tsx
│   │   │   └── ArticleCard.tsx
│   │   └── ui/                # shadcn components
│   ├── hooks/
│   │   ├── usePriceSocket.ts
│   │   ├── useArticles.ts
│   │   └── useCandles.ts
│   ├── stores/
│   │   ├── priceStore.ts
│   │   └── uiStore.ts
│   └── lib/
│       ├── api.ts
│       └── websocket.ts
```

### Phase 3: Core Components

#### PriceChart Component

```typescript
// src/components/charts/PriceChart.tsx
'use client';

import { useEffect, useRef, useCallback } from 'react';
import { createChart, IChartApi, ISeriesApi } from 'lightweight-charts';
import { usePriceSocket } from '@/hooks/usePriceSocket';
import { useCandles } from '@/hooks/useCandles';

interface PriceChartProps {
  symbol: string;
  interval: string;
}

export function PriceChart({ symbol, interval }: PriceChartProps) {
  const chartContainerRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<IChartApi | null>(null);
  const seriesRef = useRef<ISeriesApi<'Candlestick'> | null>(null);
  
  const { data: historicalData } = useCandles(symbol, interval);
  const liveTick = usePriceSocket(symbol);
  
  // Initialize chart
  useEffect(() => {
    if (!chartContainerRef.current) return;
    
    const chart = createChart(chartContainerRef.current, {
      width: chartContainerRef.current.clientWidth,
      height: 400,
      layout: {
        background: { color: '#131722' },
        textColor: '#d1d4dc',
      },
      grid: {
        vertLines: { color: '#1f2937' },
        horzLines: { color: '#1f2937' },
      },
      timeScale: {
        timeVisible: true,
        secondsVisible: false,
      },
    });
    
    chartRef.current = chart;
    
    const series = chart.addCandlestickSeries({
      upColor: '#22c55e',
      downColor: '#ef4444',
      borderVisible: false,
      wickUpColor: '#22c55e',
      wickDownColor: '#ef4444',
    });
    
    seriesRef.current = series;
    
    // Handle resize
    const handleResize = () => {
      if (chartContainerRef.current) {
        chart.applyOptions({ width: chartContainerRef.current.clientWidth });
      }
    };
    
    window.addEventListener('resize', handleResize);
    
    return () => {
      window.removeEventListener('resize', handleResize);
      chart.remove();
    };
  }, []);
  
  // Update with historical data
  useEffect(() => {
    if (seriesRef.current && historicalData) {
      seriesRef.current.setData(historicalData);
    }
  }, [historicalData]);
  
  // Update with live tick
  useEffect(() => {
    if (seriesRef.current && liveTick) {
      seriesRef.current.update({
        time: Math.floor(liveTick.timestamp / 1000) as any,
        open: liveTick.price,
        high: liveTick.price,
        low: liveTick.price,
        close: liveTick.price,
      });
    }
  }, [liveTick]);
  
  return (
    <div className="w-full bg-gray-900 rounded-lg p-4">
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-xl font-bold text-white">{symbol}</h2>
        <span className="text-2xl font-mono text-green-400">
          ${liveTick?.price.toFixed(2) ?? '---'}
        </span>
      </div>
      <div ref={chartContainerRef} className="w-full" />
    </div>
  );
}
```

---

## Adaptation Strategy

### What to Leverage from Existing Projects

1. **Chart Configuration**
   - Copy chart styling from lightweight-charts examples
   - Adapt candlestick rendering logic
   
2. **WebSocket Patterns**
   - Use reconnection logic from socket.io examples
   - Adapt message handling patterns
   
3. **State Management**
   - Copy store patterns from Zustand examples
   - Adapt for our specific data structures

### What to Build Custom

1. **API Integration Layer**
   - Custom hooks for our specific endpoints
   - Error handling for our backend
   
2. **News Sentiment Display**
   - Custom sentiment badges
   - Article-chart linking logic
   
3. **Symbol/Timeframe Selection**
   - Custom dropdowns matching our API

### Code Adaptation Checklist

| Component | Source | Adaptation Needed |
|-----------|--------|-------------------|
| Chart rendering | lightweight-charts docs | Minimal - use as-is |
| WebSocket hook | react-use-websocket | Adapt for Spring STOMP |
| News cards | shadcn/ui Card | Add sentiment styling |
| Symbol selector | shadcn/ui Select | Add our symbols |
| Data fetching | TanStack Query | Configure for our API |

---

## PowerShell Commands

### Project Setup

```powershell
# Create Next.js project
npx create-next-app@latest trading-frontend --typescript --tailwind --eslint --app --src-dir
Set-Location trading-frontend

# Install dependencies
npm install lightweight-charts @tanstack/react-query zustand socket.io-client axios
npm install -D @types/sockjs-client

# Initialize shadcn/ui
npx shadcn-ui@latest init

# Add common components
npx shadcn-ui@latest add button card table tabs select dropdown-menu
```

### Development

```powershell
# Start development server
npm run dev

# Run linting
npm run lint

# Run type checking
npx tsc --noEmit

# Build for production
npm run build

# Start production server
npm run start
```

### Testing

```powershell
# Install testing libraries
npm install -D jest @testing-library/react @testing-library/jest-dom jest-environment-jsdom

# Run tests
npm run test

# Run tests with coverage
npm run test -- --coverage
```

---

## References

### Official Documentation

- [Next.js Documentation](https://nextjs.org/docs)
- [TradingView Lightweight Charts](https://tradingview.github.io/lightweight-charts/)
- [TanStack Query](https://tanstack.com/query/latest)
- [Zustand](https://docs.pmnd.rs/zustand/getting-started/introduction)
- [shadcn/ui](https://ui.shadcn.com/)

### Tutorials & Guides

- [Real-time Trading Chart with React](https://tradingview.github.io/lightweight-charts/tutorials/react/simple)
- [WebSocket with React Hooks](https://blog.logrocket.com/websocket-tutorial-real-time-node-react/)
- [Building Trading Dashboards](https://www.sitepoint.com/build-real-time-stock-dashboard-react/)

### Related Project Documents

- [CoreRequirements.md](../CoreRequirements.md) - Business requirements
- [Architecture.md](../Architecture.md) - System architecture
- [Features.md](../Features.md) - Feature specifications
- [Phase3-ImplementationGuide.md](./Phase3-ImplementationGuide.md) - TradingView integration
