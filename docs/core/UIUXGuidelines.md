# UI/UX Design Guidelines

**Trading Platform - Comprehensive UI/UX Documentation**

---

## Table of Contents

1. [Overview](#overview)
2. [Design Principles](#design-principles)
3. [Visual Identity](#visual-identity)
4. [Color System](#color-system)
5. [Typography](#typography)
6. [Spacing & Layout](#spacing--layout)
7. [Component Library](#component-library)
8. [Page Templates](#page-templates)
9. [Real-time Data Visualization](#real-time-data-visualization)
10. [Responsive Design](#responsive-design)
11. [Accessibility Standards](#accessibility-standards)
12. [Interaction Patterns](#interaction-patterns)
13. [Loading & Error States](#loading--error-states)
14. [Motion & Animation](#motion--animation)
15. [Implementation Guide](#implementation-guide)
16. [References](#references)

---

## Overview

### Purpose

This document establishes comprehensive UI/UX guidelines for the Trading Platform, ensuring:

1. **Consistent User Experience** - Unified design language across all pages and components
2. **Professional Trading Interface** - Industry-standard patterns for financial applications
3. **Real-time Data Display** - Optimized for live price updates and streaming data
4. **Accessibility Compliance** - WCAG 2.1 AA standards for inclusive design
5. **Responsive Design** - Seamless experience across desktop, tablet, and mobile

### Target Users

| User Type | Primary Goals | Key Features |
|-----------|--------------|--------------|
| **Traders** | Monitor prices, view charts, quick decisions | Real-time charts, price alerts, quick navigation |
| **Analysts** | Research news, analyze sentiment, export data | News feed, sentiment indicators, data tables |
| **Administrators** | Manage sources, view metrics | Admin dashboard, configuration panels |

### Design System Stack

Based on [Frontend-ResearchGuide.md](../Frontend-ResearchGuide.md):

| Technology | Purpose |
|------------|---------|
| **Next.js 14+** | Framework with App Router |
| **TypeScript** | Type-safe development |
| **TailwindCSS** | Utility-first styling |
| **shadcn/ui** | Pre-built accessible components |
| **Radix UI** | Unstyled accessible primitives |
| **lightweight-charts** | TradingView-style charts |

---

## Design Principles

### 1. Data-First Design

> Information density over decoration. Every pixel should serve a purpose.

- Prioritize data visibility
- Minimize decorative elements
- Use whitespace strategically to group information
- Support scannable layouts

### 2. Real-time Clarity

> Users must instantly understand what's changing and what it means.

- Clear visual hierarchy for live data
- Distinct update animations
- Color-coded price movements (green/red)
- Timestamp visibility

### 3. Professional Trust

> Financial applications require credibility and reliability signals.

- Clean, minimal aesthetic
- Consistent behavior
- No jarring animations
- Error handling that builds confidence

### 4. Efficiency

> Reduce clicks, support keyboard navigation, optimize for power users.

- Keyboard shortcuts for common actions
- Quick symbol search
- Persistent user preferences
- Customizable layouts

### 5. Progressive Disclosure

> Show essential information first, details on demand.

- Summary views → detail views
- Collapsible panels
- Tooltips for additional context
- Modal dialogs for complex actions

---

## Visual Identity

### Brand Attributes

| Attribute | Description |
|-----------|-------------|
| **Modern** | Clean lines, contemporary typography, current design trends |
| **Professional** | Financial-grade interface, trustworthy appearance |
| **Data-focused** | Information-rich layouts, clear data visualization |
| **Accessible** | Inclusive design, readable, usable by all |

### Logo Usage

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│   ╔══════════════╗                                         │
│   ║   TRADING    ║   Primary Logo: Used in header          │
│   ║   PLATFORM   ║   Min size: 120px width                 │
│   ╚══════════════╝   Clear space: 16px around              │
│                                                             │
│   ╔════╗              Icon: Used in favicon, mobile        │
│   ║ TP ║              Min size: 32px                       │
│   ╚════╝                                                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Iconography

- **Icon Library**: Lucide Icons (consistent with shadcn/ui)
- **Icon Size**: 16px (small), 20px (default), 24px (large)
- **Icon Style**: Stroke width 2px, rounded corners
- **Color**: Inherit from parent text color

```tsx
// Icon usage example
import { TrendingUp, TrendingDown, Activity } from 'lucide-react';

<TrendingUp className="h-5 w-5 text-success" />
<TrendingDown className="h-5 w-5 text-destructive" />
<Activity className="h-5 w-5 text-muted-foreground" />
```

---

## Color System

### Theme Support

The platform supports both **Light** and **Dark** themes with user preference persistence.

### Dark Theme (Default)

Dark theme is the default for trading applications, reducing eye strain during extended use.

```css
/* Dark Theme - CSS Custom Properties */
:root[data-theme="dark"] {
  /* Background Colors */
  --background: 222.2 84% 4.9%;        /* #0a0b0d - Main background */
  --background-secondary: 217.2 32.6% 7.5%;  /* #0f1419 - Cards, panels */
  --background-tertiary: 215 20% 11%;  /* #161b22 - Elevated surfaces */
  
  /* Foreground Colors */
  --foreground: 210 40% 98%;           /* #f8fafc - Primary text */
  --foreground-muted: 215 20.2% 65.1%; /* #8b949e - Secondary text */
  
  /* Brand Colors */
  --primary: 217.2 91.2% 59.8%;        /* #3b82f6 - Primary actions */
  --primary-foreground: 222.2 47.4% 11.2%; /* Button text on primary */
  
  /* Semantic Colors */
  --success: 142.1 70.6% 45.3%;        /* #22c55e - Price up, positive */
  --destructive: 0 84.2% 60.2%;        /* #ef4444 - Price down, errors */
  --warning: 38 92% 50%;               /* #f59e0b - Warnings */
  --info: 199 89% 48%;                 /* #0ea5e9 - Info states */
  
  /* Chart Colors */
  --chart-bullish: 142.1 70.6% 45.3%;  /* #22c55e - Green candles */
  --chart-bearish: 0 84.2% 60.2%;      /* #ef4444 - Red candles */
  --chart-grid: 215 20% 20%;           /* Grid lines */
  --chart-crosshair: 215 20% 50%;      /* Crosshair color */
  
  /* Border & Divider */
  --border: 217.2 32.6% 17.5%;         /* #1f2937 - Borders */
  --ring: 212.7 26.8% 83.9%;           /* Focus ring */
  
  /* Component Specific */
  --card: 217.2 32.6% 7.5%;            /* Card backgrounds */
  --popover: 222.2 84% 4.9%;           /* Dropdown backgrounds */
  --muted: 217.2 32.6% 17.5%;          /* Muted backgrounds */
  --accent: 217.2 32.6% 17.5%;         /* Hover states */
}
```

### Light Theme

```css
/* Light Theme - CSS Custom Properties */
:root[data-theme="light"] {
  /* Background Colors */
  --background: 0 0% 100%;             /* #ffffff - Main background */
  --background-secondary: 210 40% 98%; /* #f8fafc - Cards, panels */
  --background-tertiary: 214.3 31.8% 91.4%; /* #e2e8f0 - Elevated */
  
  /* Foreground Colors */
  --foreground: 222.2 84% 4.9%;        /* #0a0b0d - Primary text */
  --foreground-muted: 215.4 16.3% 46.9%; /* #64748b - Secondary text */
  
  /* Brand Colors */
  --primary: 221.2 83.2% 53.3%;        /* #2563eb - Primary actions */
  --primary-foreground: 210 40% 98%;   /* Button text on primary */
  
  /* Semantic Colors */
  --success: 142.1 76.2% 36.3%;        /* #16a34a - Price up */
  --destructive: 0 72.2% 50.6%;        /* #dc2626 - Price down */
  --warning: 32.1 94.6% 43.7%;         /* #d97706 - Warnings */
  --info: 199 89% 48%;                 /* #0ea5e9 - Info states */
  
  /* Chart Colors */
  --chart-bullish: 142.1 76.2% 36.3%;  /* Green candles */
  --chart-bearish: 0 72.2% 50.6%;      /* Red candles */
  --chart-grid: 214.3 31.8% 91.4%;     /* Grid lines */
  --chart-crosshair: 215 20% 50%;      /* Crosshair */
  
  /* Border & Divider */
  --border: 214.3 31.8% 91.4%;         /* #e2e8f0 - Borders */
  --ring: 222.2 84% 4.9%;              /* Focus ring */
}
```

### Price Movement Colors

```tsx
// Price change color utility
type PriceDirection = 'up' | 'down' | 'neutral';

const priceColors: Record<PriceDirection, string> = {
  up: 'text-success',      // Green (#22c55e)
  down: 'text-destructive', // Red (#ef4444)
  neutral: 'text-muted-foreground',
};

// Candlestick colors
const candleColors = {
  bullish: {
    body: 'hsl(var(--chart-bullish))',
    wick: 'hsl(var(--chart-bullish))',
  },
  bearish: {
    body: 'hsl(var(--chart-bearish))',
    wick: 'hsl(var(--chart-bearish))',
  },
};
```

### Sentiment Colors

```tsx
// Sentiment indicator colors
const sentimentColors = {
  bullish: {
    bg: 'bg-success/10',
    text: 'text-success',
    border: 'border-success/50',
  },
  bearish: {
    bg: 'bg-destructive/10',
    text: 'text-destructive',
    border: 'border-destructive/50',
  },
  neutral: {
    bg: 'bg-muted',
    text: 'text-muted-foreground',
    border: 'border-muted',
  },
};
```

### Color Accessibility

All color combinations meet WCAG 2.1 AA contrast requirements:

| Combination | Contrast Ratio | Pass/Fail |
|-------------|----------------|-----------|
| Foreground on Background | 15.8:1 | ✅ AAA |
| Muted on Background | 7.2:1 | ✅ AAA |
| Success on Background | 4.9:1 | ✅ AA |
| Destructive on Background | 5.4:1 | ✅ AA |
| Primary on Background | 5.8:1 | ✅ AA |

---

## Typography

### Font Stack

```css
:root {
  /* Primary font - UI elements */
  --font-sans: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 
               'Helvetica Neue', Arial, sans-serif;
  
  /* Monospace - Numbers, code, prices */
  --font-mono: 'JetBrains Mono', 'SF Mono', 'Fira Code', Consolas, 
               'Liberation Mono', Menlo, monospace;
}
```

### Type Scale

```css
/* Type scale using TailwindCSS default values for consistency */
.text-xs    { font-size: 0.75rem;  line-height: 1rem; }     /* 12px */
.text-sm    { font-size: 0.875rem; line-height: 1.25rem; }  /* 14px */
.text-base  { font-size: 1rem;     line-height: 1.5rem; }   /* 16px */
.text-lg    { font-size: 1.125rem; line-height: 1.75rem; }  /* 18px */
.text-xl    { font-size: 1.25rem;  line-height: 1.75rem; }  /* 20px */
.text-2xl   { font-size: 1.5rem;   line-height: 2rem; }     /* 24px */
.text-3xl   { font-size: 1.875rem; line-height: 2.25rem; }  /* 30px */
.text-4xl   { font-size: 2.25rem;  line-height: 2.5rem; }   /* 36px */
```

### Font Weights

```css
.font-normal   { font-weight: 400; }  /* Body text */
.font-medium   { font-weight: 500; }  /* Labels, emphasis */
.font-semibold { font-weight: 600; }  /* Headings, buttons */
.font-bold     { font-weight: 700; }  /* Strong emphasis */
```

### Typography Components

```tsx
// Heading styles
const headingStyles = {
  h1: 'text-3xl font-bold tracking-tight',
  h2: 'text-2xl font-semibold tracking-tight',
  h3: 'text-xl font-semibold',
  h4: 'text-lg font-medium',
};

// Price display (monospace)
const priceStyles = {
  large: 'font-mono text-2xl font-semibold tabular-nums',
  default: 'font-mono text-lg font-medium tabular-nums',
  small: 'font-mono text-sm tabular-nums',
};

// Example usage
<h1 className={headingStyles.h1}>Dashboard</h1>
<span className={`${priceStyles.large} ${priceColors.up}`}>
  $50,123.45
</span>
```

### Numeric Display

All numeric values use **tabular figures** for alignment:

```tsx
// Tabular figures for aligned numbers
<span className="font-mono tabular-nums">50,123.45</span>
<span className="font-mono tabular-nums">1,234.56</span>
<span className="font-mono tabular-nums">987.00</span>
```

---

## Spacing & Layout

### Spacing Scale

```css
/* 4px base unit spacing scale */
.space-0   { --space: 0; }        /* 0px */
.space-1   { --space: 0.25rem; }  /* 4px */
.space-2   { --space: 0.5rem; }   /* 8px */
.space-3   { --space: 0.75rem; }  /* 12px */
.space-4   { --space: 1rem; }     /* 16px */
.space-5   { --space: 1.25rem; }  /* 20px */
.space-6   { --space: 1.5rem; }   /* 24px */
.space-8   { --space: 2rem; }     /* 32px */
.space-10  { --space: 2.5rem; }   /* 40px */
.space-12  { --space: 3rem; }     /* 48px */
.space-16  { --space: 4rem; }     /* 64px */
```

### Grid System

```tsx
// Main layout grid
const layoutGrid = {
  sidebar: 'w-64',          // 256px sidebar
  mainContent: 'flex-1',    // Flexible main area
  rightPanel: 'w-80',       // 320px right panel (optional)
};

// Content grid (12-column)
const contentGrid = 'grid grid-cols-12 gap-4';

// Common patterns
const gridPatterns = {
  twoColumn: 'grid grid-cols-1 md:grid-cols-2 gap-4',
  threeColumn: 'grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4',
  chartLayout: 'grid grid-cols-12 gap-4', // Chart 8 cols, sidebar 4 cols
};
```

### Layout Components

```tsx
// Page container
<div className="min-h-screen bg-background">
  {/* Header */}
  <header className="sticky top-0 z-50 h-14 border-b border-border bg-background/95 backdrop-blur">
    {/* Header content */}
  </header>
  
  {/* Main layout */}
  <div className="flex">
    {/* Sidebar */}
    <aside className="hidden lg:block w-64 border-r border-border">
      {/* Sidebar content */}
    </aside>
    
    {/* Main content */}
    <main className="flex-1 p-4 lg:p-6">
      {/* Page content */}
    </main>
  </div>
</div>
```

### Card Spacing

```tsx
// Standard card padding
const cardPadding = {
  compact: 'p-3',      // 12px - Dense data displays
  default: 'p-4',      // 16px - Standard cards
  relaxed: 'p-6',      // 24px - Feature cards
};

// Card with header
<Card>
  <CardHeader className="pb-3">
    <CardTitle className="text-lg font-semibold">Title</CardTitle>
    <CardDescription>Description text</CardDescription>
  </CardHeader>
  <CardContent className="pt-0">
    {/* Content */}
  </CardContent>
</Card>
```

---

## Component Library

### Buttons

```tsx
// Button variants (using shadcn/ui)
const buttonVariants = {
  default: 'bg-primary text-primary-foreground hover:bg-primary/90',
  destructive: 'bg-destructive text-destructive-foreground hover:bg-destructive/90',
  outline: 'border border-input bg-background hover:bg-accent',
  secondary: 'bg-secondary text-secondary-foreground hover:bg-secondary/80',
  ghost: 'hover:bg-accent hover:text-accent-foreground',
  link: 'text-primary underline-offset-4 hover:underline',
};

// Button sizes
const buttonSizes = {
  sm: 'h-8 px-3 text-xs',
  default: 'h-10 px-4 py-2',
  lg: 'h-12 px-8 text-lg',
  icon: 'h-10 w-10',
};

// Usage
<Button variant="default" size="default">Primary Action</Button>
<Button variant="outline" size="sm">Secondary</Button>
<Button variant="ghost" size="icon"><Settings className="h-4 w-4" /></Button>
```

### Cards

```tsx
// Trading card with price display
function PriceCard({ symbol, price, change, changePercent }: PriceCardProps) {
  const isPositive = change >= 0;
  
  return (
    <Card className="hover:bg-accent/50 transition-colors cursor-pointer">
      <CardContent className="p-4">
        <div className="flex justify-between items-start">
          <div>
            <h3 className="font-semibold text-lg">{symbol}</h3>
            <p className="text-muted-foreground text-sm">Binance</p>
          </div>
          <div className="text-right">
            <p className="font-mono text-xl font-semibold tabular-nums">
              ${price.toLocaleString()}
            </p>
            <p className={cn(
              "font-mono text-sm tabular-nums",
              isPositive ? "text-success" : "text-destructive"
            )}>
              {isPositive ? '+' : ''}{changePercent.toFixed(2)}%
            </p>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
```

### Tables

```tsx
// Data table styling
const tableStyles = {
  wrapper: 'rounded-md border',
  table: 'w-full',
  header: 'border-b bg-muted/50',
  headerCell: 'h-10 px-4 text-left align-middle font-medium text-muted-foreground',
  body: 'divide-y divide-border',
  row: 'hover:bg-muted/50 transition-colors',
  cell: 'px-4 py-3 align-middle',
  cellNumeric: 'px-4 py-3 text-right font-mono tabular-nums',
};

// Price table row
<tr className={tableStyles.row}>
  <td className={tableStyles.cell}>{symbol}</td>
  <td className={tableStyles.cellNumeric}>{price}</td>
  <td className={cn(tableStyles.cellNumeric, change >= 0 ? 'text-success' : 'text-destructive')}>
    {change >= 0 ? '+' : ''}{changePercent}%
  </td>
  <td className={tableStyles.cellNumeric}>{volume}</td>
</tr>
```

### Forms

```tsx
// Input styling
const inputStyles = {
  base: 'flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm',
  focus: 'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
  placeholder: 'placeholder:text-muted-foreground',
  disabled: 'disabled:cursor-not-allowed disabled:opacity-50',
};

// Symbol search input
<div className="relative">
  <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
  <Input 
    placeholder="Search symbols..." 
    className="pl-9"
  />
</div>
```

### Tabs

```tsx
// Timeframe selector tabs
<Tabs defaultValue="1h" className="w-full">
  <TabsList className="grid grid-cols-6 w-full max-w-md">
    <TabsTrigger value="1m">1m</TabsTrigger>
    <TabsTrigger value="5m">5m</TabsTrigger>
    <TabsTrigger value="15m">15m</TabsTrigger>
    <TabsTrigger value="1h">1H</TabsTrigger>
    <TabsTrigger value="4h">4H</TabsTrigger>
    <TabsTrigger value="1d">1D</TabsTrigger>
  </TabsList>
</Tabs>
```

### Badges & Pills

```tsx
// Sentiment badge
function SentimentBadge({ sentiment }: { sentiment: 'bullish' | 'bearish' | 'neutral' }) {
  const styles = {
    bullish: 'bg-success/10 text-success border-success/20',
    bearish: 'bg-destructive/10 text-destructive border-destructive/20',
    neutral: 'bg-muted text-muted-foreground border-border',
  };
  
  const icons = {
    bullish: <TrendingUp className="h-3 w-3 mr-1" />,
    bearish: <TrendingDown className="h-3 w-3 mr-1" />,
    neutral: <Minus className="h-3 w-3 mr-1" />,
  };
  
  return (
    <Badge variant="outline" className={cn('font-medium', styles[sentiment])}>
      {icons[sentiment]}
      {sentiment.charAt(0).toUpperCase() + sentiment.slice(1)}
    </Badge>
  );
}
```

---

## Page Templates

### Dashboard Layout

```
┌─────────────────────────────────────────────────────────────────────────┐
│  Header: Logo | Symbol Search | Notifications | User Menu | Theme      │
├────────────┬────────────────────────────────────────────────────────────┤
│            │                                                            │
│  Sidebar   │  ┌────────────────────────┐ ┌────────────────────────────┐ │
│            │  │  Price Overview Cards   │ │   Market Summary          │ │
│  - Dashboard│  │  (BTCUSDT, ETHUSDT...) │ │   - Top Gainers           │ │
│  - Charts  │  └────────────────────────┘ │   - Top Losers            │ │
│  - News    │                             │   - Volume Leaders        │ │
│  - Analysis│  ┌────────────────────────────────────────────────────────┤
│  - Settings│  │                                                        │ │
│            │  │              Main Price Chart                          │ │
│            │  │              (TradingView style)                       │ │
│            │  │                                                        │ │
│            │  ├────────────────────────────────────────────────────────┤
│            │  │  ┌──────────────────────┐ ┌──────────────────────────┐ │
│            │  │  │    Recent News       │ │   Sentiment Overview     │ │
│            │  │  │    - Article 1       │ │   - Bullish: 65%         │ │
│            │  │  │    - Article 2       │ │   - Bearish: 25%         │ │
│            │  │  │    - Article 3       │ │   - Neutral: 10%         │ │
│            │  │  └──────────────────────┘ └──────────────────────────┘ │
└────────────┴────────────────────────────────────────────────────────────┘
```

### Chart Page Layout

```
┌─────────────────────────────────────────────────────────────────────────┐
│  Header                                                                  │
├─────────────────────────────────────────────────────────────────────────┤
│  Toolbar: [Symbol Selector] [Timeframe Tabs] [Indicators] [Fullscreen] │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│                                                                          │
│                                                                          │
│                        Main Chart Area                                   │
│                    (80% viewport height)                                 │
│                                                                          │
│                                                                          │
│                                                                          │
├─────────────────────────────────────────────────────────────────────────┤
│  Order Book (optional) │ Recent Trades │ Symbol Info │ Related News    │
└─────────────────────────────────────────────────────────────────────────┘
```

### News Feed Layout

```
┌─────────────────────────────────────────────────────────────────────────┐
│  Header                                                                  │
├────────────┬────────────────────────────────────────────────────────────┤
│            │  ┌─────────────────────────────────────────────────────┐   │
│  Filters   │  │  Filter Bar: [Source] [Symbol] [Sentiment] [Date]   │   │
│            │  └─────────────────────────────────────────────────────┘   │
│  □ All     │                                                            │
│  □ Bitcoin │  ┌─────────────────────────────────────────────────────┐   │
│  □ Ethereum│  │  ┌─────────────────────────────────────────────────┐│   │
│  □ Altcoins│  │  │ Article Card                                    ││   │
│            │  │  │ [Source] [Time] [Sentiment Badge]               ││   │
│  Sources   │  │  │ Title goes here...                              ││   │
│  □ CoinDesk│  │  │ Excerpt text with first 200 characters...       ││   │
│  □ Decrypt │  │  │ [Symbol Tags: BTC, ETH]                         ││   │
│  □ Others  │  │  └─────────────────────────────────────────────────┘│   │
│            │  │                                                      │   │
│  Sentiment │  │  ┌─────────────────────────────────────────────────┐│   │
│  ○ All     │  │  │ Article Card                                    ││   │
│  ○ Bullish │  │  │ ...                                             ││   │
│  ○ Bearish │  │  └─────────────────────────────────────────────────┘│   │
│  ○ Neutral │  │                                                      │   │
│            │  └─────────────────────────────────────────────────────┘   │
└────────────┴────────────────────────────────────────────────────────────┘
```

---

## Real-time Data Visualization

### Price Chart Configuration

```tsx
// Lightweight Charts configuration
const chartConfig = {
  layout: {
    background: { type: ColorType.Solid, color: 'transparent' },
    textColor: 'hsl(var(--foreground))',
  },
  grid: {
    vertLines: { color: 'hsl(var(--chart-grid))' },
    horzLines: { color: 'hsl(var(--chart-grid))' },
  },
  timeScale: {
    borderColor: 'hsl(var(--border))',
    timeVisible: true,
    secondsVisible: false,
  },
  rightPriceScale: {
    borderColor: 'hsl(var(--border))',
  },
  crosshair: {
    mode: CrosshairMode.Normal,
    vertLine: {
      color: 'hsl(var(--chart-crosshair))',
      width: 1,
      style: LineStyle.Dashed,
    },
    horzLine: {
      color: 'hsl(var(--chart-crosshair))',
      width: 1,
      style: LineStyle.Dashed,
    },
  },
};

// Candlestick series options
const candlestickOptions = {
  upColor: 'hsl(var(--chart-bullish))',
  downColor: 'hsl(var(--chart-bearish))',
  borderUpColor: 'hsl(var(--chart-bullish))',
  borderDownColor: 'hsl(var(--chart-bearish))',
  wickUpColor: 'hsl(var(--chart-bullish))',
  wickDownColor: 'hsl(var(--chart-bearish))',
};
```

### Real-time Price Updates

```tsx
// Price update animation
function PriceDisplay({ price, previousPrice }: PriceDisplayProps) {
  const [flash, setFlash] = useState<'up' | 'down' | null>(null);
  
  useEffect(() => {
    if (previousPrice && price !== previousPrice) {
      setFlash(price > previousPrice ? 'up' : 'down');
      const timer = setTimeout(() => setFlash(null), 300);
      return () => clearTimeout(timer);
    }
  }, [price, previousPrice]);
  
  return (
    <span className={cn(
      "font-mono text-2xl font-semibold tabular-nums transition-colors duration-300",
      flash === 'up' && 'text-success bg-success/10',
      flash === 'down' && 'text-destructive bg-destructive/10',
    )}>
      ${price.toLocaleString(undefined, { minimumFractionDigits: 2 })}
    </span>
  );
}
```

### Streaming Indicators

```tsx
// Connection status indicator
function ConnectionStatus({ status }: { status: 'connected' | 'connecting' | 'disconnected' }) {
  const statusConfig = {
    connected: { color: 'bg-success', text: 'Live', pulse: true },
    connecting: { color: 'bg-warning', text: 'Connecting...', pulse: true },
    disconnected: { color: 'bg-destructive', text: 'Disconnected', pulse: false },
  };
  
  const { color, text, pulse } = statusConfig[status];
  
  return (
    <div className="flex items-center gap-2">
      <span className={cn(
        "h-2 w-2 rounded-full",
        color,
        pulse && "animate-pulse"
      )} />
      <span className="text-xs text-muted-foreground">{text}</span>
    </div>
  );
}
```

### Data Freshness Indicators

```tsx
// Last updated timestamp
function LastUpdated({ timestamp }: { timestamp: Date }) {
  const [timeAgo, setTimeAgo] = useState('');
  
  useEffect(() => {
    const update = () => {
      const seconds = Math.floor((Date.now() - timestamp.getTime()) / 1000);
      if (seconds < 60) setTimeAgo(`${seconds}s ago`);
      else if (seconds < 3600) setTimeAgo(`${Math.floor(seconds / 60)}m ago`);
      else setTimeAgo(timestamp.toLocaleTimeString());
    };
    
    update();
    const interval = setInterval(update, 1000);
    return () => clearInterval(interval);
  }, [timestamp]);
  
  return (
    <span className="text-xs text-muted-foreground">
      Updated {timeAgo}
    </span>
  );
}
```

---

## Responsive Design

### Breakpoints

```css
/* TailwindCSS default breakpoints */
/* sm: 640px  - Large phones */
/* md: 768px  - Tablets */
/* lg: 1024px - Laptops */
/* xl: 1280px - Desktops */
/* 2xl: 1536px - Large monitors */
```

### Responsive Patterns

```tsx
// Mobile-first navigation
<nav className="flex items-center">
  {/* Mobile: Hamburger menu */}
  <Button variant="ghost" size="icon" className="lg:hidden">
    <Menu className="h-5 w-5" />
  </Button>
  
  {/* Desktop: Full navigation */}
  <div className="hidden lg:flex items-center gap-4">
    <NavLink href="/dashboard">Dashboard</NavLink>
    <NavLink href="/charts">Charts</NavLink>
    <NavLink href="/news">News</NavLink>
  </div>
</nav>

// Responsive grid
<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
  {symbols.map(symbol => <PriceCard key={symbol.id} {...symbol} />)}
</div>

// Responsive chart sizing
<div className="h-[300px] md:h-[400px] lg:h-[500px] xl:h-[600px]">
  <PriceChart symbol={selectedSymbol} />
</div>
```

### Mobile-Specific Adjustments

```tsx
// Touch-friendly targets (minimum 44x44px)
const mobileButtonStyles = 'min-h-[44px] min-w-[44px]';

// Simplified mobile layout
function MobileLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="lg:hidden">
      {/* Sticky header with essential info */}
      <header className="sticky top-0 z-50 bg-background border-b p-4">
        <div className="flex justify-between items-center">
          <SymbolSelector />
          <PriceDisplay />
        </div>
      </header>
      
      {/* Swipeable tabs for navigation */}
      <Tabs className="w-full">
        <TabsList className="w-full grid grid-cols-3">
          <TabsTrigger value="chart">Chart</TabsTrigger>
          <TabsTrigger value="news">News</TabsTrigger>
          <TabsTrigger value="analysis">Analysis</TabsTrigger>
        </TabsList>
      </Tabs>
      
      {/* Content area */}
      <main className="p-4">{children}</main>
      
      {/* Bottom navigation */}
      <nav className="fixed bottom-0 left-0 right-0 bg-background border-t">
        <div className="flex justify-around py-2">
          <NavButton icon={<Home />} label="Home" />
          <NavButton icon={<BarChart3 />} label="Charts" />
          <NavButton icon={<Newspaper />} label="News" />
          <NavButton icon={<User />} label="Profile" />
        </div>
      </nav>
    </div>
  );
}
```

---

## Accessibility Standards

### WCAG 2.1 AA Compliance

| Requirement | Implementation |
|-------------|----------------|
| **Color Contrast** | Minimum 4.5:1 for normal text, 3:1 for large text |
| **Focus Indicators** | Visible focus ring on all interactive elements |
| **Keyboard Navigation** | Full keyboard accessibility for all features |
| **Screen Reader Support** | ARIA labels, roles, and live regions |
| **Motion Sensitivity** | Respect `prefers-reduced-motion` |

### Focus Management

```tsx
// Focus ring styling
const focusRing = 'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2';

// Skip link for keyboard users
<a 
  href="#main-content" 
  className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 z-50 bg-primary text-primary-foreground px-4 py-2 rounded"
>
  Skip to main content
</a>
```

### ARIA Labels

```tsx
// Price change with screen reader context
<span 
  aria-label={`Bitcoin price ${price} dollars, ${change >= 0 ? 'up' : 'down'} ${Math.abs(changePercent)} percent`}
  className={priceStyles}
>
  ${price} ({change >= 0 ? '+' : ''}{changePercent}%)
</span>

// Live region for real-time updates
<div 
  role="status" 
  aria-live="polite" 
  aria-atomic="true"
  className="sr-only"
>
  Price updated to {price}
</div>

// Chart with accessible description
<figure aria-labelledby="chart-title" aria-describedby="chart-description">
  <figcaption id="chart-title" className="sr-only">
    Bitcoin price chart for the last 24 hours
  </figcaption>
  <p id="chart-description" className="sr-only">
    Interactive candlestick chart showing price ranging from {low} to {high}
  </p>
  <PriceChart {...chartProps} />
</figure>
```

### Reduced Motion

```tsx
// Respect reduced motion preference
const prefersReducedMotion = typeof window !== 'undefined' 
  ? window.matchMedia('(prefers-reduced-motion: reduce)').matches 
  : false;

// Conditional animations
<span className={cn(
  "transition-colors",
  !prefersReducedMotion && "duration-300"
)}>
  {content}
</span>

// CSS approach
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
```

---

## Interaction Patterns

### Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `/` | Focus search input |
| `Esc` | Close modal / Clear search |
| `↑` / `↓` | Navigate lists |
| `Enter` | Select / Confirm |
| `1-6` | Switch timeframe (1m, 5m, 15m, 1h, 4h, 1d) |
| `F` | Toggle fullscreen chart |
| `T` | Toggle theme |

```tsx
// Keyboard shortcut implementation
function useKeyboardShortcuts() {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Ignore if typing in input
      if (['INPUT', 'TEXTAREA'].includes(document.activeElement?.tagName ?? '')) {
        return;
      }
      
      switch (e.key) {
        case '/':
          e.preventDefault();
          document.getElementById('symbol-search')?.focus();
          break;
        case 'f':
        case 'F':
          if (!e.ctrlKey && !e.metaKey) {
            toggleFullscreen();
          }
          break;
        case 't':
        case 'T':
          if (!e.ctrlKey && !e.metaKey) {
            toggleTheme();
          }
          break;
      }
    };
    
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, []);
}
```

### Hover States

```tsx
// Interactive card hover
const cardHover = 'transition-all duration-200 hover:bg-accent/50 hover:border-accent';

// Button hover with feedback
const buttonHover = 'transition-colors hover:bg-primary/90 active:bg-primary/80';

// Link hover
const linkHover = 'hover:text-primary hover:underline underline-offset-4';
```

### Click & Touch Feedback

```tsx
// Ripple effect component
function RippleButton({ children, ...props }: ButtonProps) {
  const [ripples, setRipples] = useState<{ x: number; y: number; id: number }[]>([]);
  
  const handleClick = (e: React.MouseEvent) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    
    setRipples(prev => [...prev, { x, y, id: Date.now() }]);
    setTimeout(() => setRipples(prev => prev.slice(1)), 600);
  };
  
  return (
    <Button onClick={handleClick} className="relative overflow-hidden" {...props}>
      {children}
      {ripples.map(ripple => (
        <span
          key={ripple.id}
          className="absolute bg-white/30 rounded-full animate-ping"
          style={{ left: ripple.x, top: ripple.y, width: 20, height: 20 }}
        />
      ))}
    </Button>
  );
}
```

---

## Loading & Error States

### Loading Patterns

```tsx
// Skeleton loading
function PriceCardSkeleton() {
  return (
    <Card>
      <CardContent className="p-4">
        <div className="flex justify-between items-start">
          <div className="space-y-2">
            <Skeleton className="h-5 w-20" />
            <Skeleton className="h-4 w-16" />
          </div>
          <div className="text-right space-y-2">
            <Skeleton className="h-6 w-28" />
            <Skeleton className="h-4 w-16 ml-auto" />
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

// Chart loading
function ChartLoading() {
  return (
    <div className="h-[400px] flex items-center justify-center bg-muted/50 rounded-lg">
      <div className="text-center">
        <Loader2 className="h-8 w-8 animate-spin mx-auto text-muted-foreground" />
        <p className="mt-2 text-sm text-muted-foreground">Loading chart data...</p>
      </div>
    </div>
  );
}

// Inline loading
function InlineLoading() {
  return (
    <span className="inline-flex items-center gap-2 text-muted-foreground">
      <Loader2 className="h-4 w-4 animate-spin" />
      Loading...
    </span>
  );
}
```

### Error States

```tsx
// Error boundary fallback
function ErrorFallback({ error, resetErrorBoundary }: FallbackProps) {
  return (
    <Card className="border-destructive/50">
      <CardContent className="p-6 text-center">
        <AlertCircle className="h-10 w-10 text-destructive mx-auto" />
        <h3 className="mt-4 font-semibold">Something went wrong</h3>
        <p className="mt-2 text-sm text-muted-foreground">
          {error.message || 'An unexpected error occurred'}
        </p>
        <Button 
          variant="outline" 
          onClick={resetErrorBoundary}
          className="mt-4"
        >
          Try again
        </Button>
      </CardContent>
    </Card>
  );
}

// Connection error
function ConnectionError({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="flex items-center gap-2 p-3 bg-destructive/10 border border-destructive/50 rounded-lg">
      <WifiOff className="h-4 w-4 text-destructive" />
      <span className="text-sm text-destructive">Connection lost</span>
      <Button variant="ghost" size="sm" onClick={onRetry}>
        Retry
      </Button>
    </div>
  );
}

// Empty state
function EmptyState({ title, description, action }: EmptyStateProps) {
  return (
    <div className="text-center py-12">
      <div className="mx-auto h-12 w-12 text-muted-foreground">
        <Inbox className="h-12 w-12" />
      </div>
      <h3 className="mt-4 text-lg font-semibold">{title}</h3>
      <p className="mt-2 text-sm text-muted-foreground max-w-sm mx-auto">
        {description}
      </p>
      {action && (
        <Button className="mt-4" onClick={action.onClick}>
          {action.label}
        </Button>
      )}
    </div>
  );
}
```

---

## Motion & Animation

### Animation Tokens

```css
/* Timing functions */
--ease-in-out: cubic-bezier(0.4, 0, 0.2, 1);
--ease-out: cubic-bezier(0, 0, 0.2, 1);
--ease-in: cubic-bezier(0.4, 0, 1, 1);
--ease-bounce: cubic-bezier(0.68, -0.55, 0.265, 1.55);

/* Durations */
--duration-fast: 150ms;
--duration-normal: 200ms;
--duration-slow: 300ms;
--duration-slower: 500ms;
```

### Common Animations

```tsx
// Fade in
const fadeIn = 'animate-in fade-in duration-200';

// Slide up
const slideUp = 'animate-in slide-in-from-bottom-4 duration-300';

// Scale
const scaleIn = 'animate-in zoom-in-95 duration-200';

// Price flash animation
const priceFlash = {
  keyframes: `
    @keyframes flash-up {
      0%, 100% { background-color: transparent; }
      50% { background-color: hsl(var(--success) / 0.2); }
    }
    @keyframes flash-down {
      0%, 100% { background-color: transparent; }
      50% { background-color: hsl(var(--destructive) / 0.2); }
    }
  `,
  classes: {
    up: 'animate-[flash-up_300ms_ease-out]',
    down: 'animate-[flash-down_300ms_ease-out]',
  }
};
```

### Chart Animations

```tsx
// Chart entry animation (lightweight-charts)
const animationConfig = {
  duration: 800,
  easing: (t: number) => 1 - Math.pow(1 - t, 3), // ease-out-cubic
};

// Crosshair movement (smooth)
chart.applyOptions({
  crosshair: {
    mode: CrosshairMode.Normal,
  },
});
```

---

## Implementation Guide

### Project Setup

```bash
# Create Next.js project
npx create-next-app@latest trading-frontend --typescript --tailwind --eslint --app --src-dir

cd trading-frontend

# Install core dependencies
npm install lightweight-charts @tanstack/react-query zustand socket.io-client

# Install UI dependencies
npx shadcn-ui@latest init
npx shadcn-ui@latest add button card table tabs badge skeleton
npx shadcn-ui@latest add dialog dropdown-menu select input

# Install icon library
npm install lucide-react

# Install fonts
npm install @fontsource/inter @fontsource/jetbrains-mono
```

### Tailwind Configuration

```typescript
// tailwind.config.ts
import type { Config } from 'tailwindcss';

const config: Config = {
  darkMode: ['class'],
  content: ['./src/**/*.{js,ts,jsx,tsx,mdx}'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['var(--font-inter)', 'sans-serif'],
        mono: ['var(--font-jetbrains-mono)', 'monospace'],
      },
      colors: {
        border: 'hsl(var(--border))',
        background: 'hsl(var(--background))',
        foreground: 'hsl(var(--foreground))',
        primary: {
          DEFAULT: 'hsl(var(--primary))',
          foreground: 'hsl(var(--primary-foreground))',
        },
        success: 'hsl(var(--success))',
        destructive: {
          DEFAULT: 'hsl(var(--destructive))',
          foreground: 'hsl(var(--destructive-foreground))',
        },
        muted: {
          DEFAULT: 'hsl(var(--muted))',
          foreground: 'hsl(var(--muted-foreground))',
        },
        card: {
          DEFAULT: 'hsl(var(--card))',
          foreground: 'hsl(var(--card-foreground))',
        },
      },
      keyframes: {
        'flash-up': {
          '0%, 100%': { backgroundColor: 'transparent' },
          '50%': { backgroundColor: 'hsl(var(--success) / 0.2)' },
        },
        'flash-down': {
          '0%, 100%': { backgroundColor: 'transparent' },
          '50%': { backgroundColor: 'hsl(var(--destructive) / 0.2)' },
        },
      },
      animation: {
        'flash-up': 'flash-up 300ms ease-out',
        'flash-down': 'flash-down 300ms ease-out',
      },
    },
  },
  plugins: [require('tailwindcss-animate')],
};

export default config;
```

### Component Structure

```
src/
├── app/
│   ├── layout.tsx          # Root layout with providers
│   ├── page.tsx            # Dashboard
│   ├── chart/
│   │   └── page.tsx        # Chart page
│   ├── news/
│   │   └── page.tsx        # News feed
│   └── globals.css         # CSS variables
├── components/
│   ├── ui/                 # shadcn/ui components
│   ├── charts/
│   │   ├── PriceChart.tsx
│   │   ├── ChartControls.tsx
│   │   └── ChartTooltip.tsx
│   ├── news/
│   │   ├── NewsFeed.tsx
│   │   ├── ArticleCard.tsx
│   │   └── SentimentBadge.tsx
│   ├── layout/
│   │   ├── Header.tsx
│   │   ├── Sidebar.tsx
│   │   └── Footer.tsx
│   └── common/
│       ├── PriceDisplay.tsx
│       ├── SymbolSelector.tsx
│       └── ConnectionStatus.tsx
├── hooks/
│   ├── usePriceSocket.ts
│   ├── useCandles.ts
│   ├── useArticles.ts
│   └── useTheme.ts
├── stores/
│   ├── priceStore.ts
│   └── uiStore.ts
├── lib/
│   ├── api.ts
│   ├── websocket.ts
│   └── utils.ts
└── styles/
    └── tokens.css          # Design tokens
```

---

## References

### Internal Documents

- [CoreRequirements.md](./CoreRequirements.md) - Business requirements
- [Architecture.md](./Architecture.md) - System architecture
- [Features.md](./Features.md) - Feature specifications
- [Frontend-ResearchGuide.md](../Frontend-ResearchGuide.md) - Technology research

### External Resources

- [TailwindCSS Documentation](https://tailwindcss.com/docs)
- [shadcn/ui Components](https://ui.shadcn.com/)
- [Radix UI Primitives](https://www.radix-ui.com/)
- [TradingView Lightweight Charts](https://tradingview.github.io/lightweight-charts/)
- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)

### Design Inspiration

- [TradingView](https://www.tradingview.com/) - Chart interface patterns
- [Binance](https://www.binance.com/) - Trading platform UX
- [Bloomberg Terminal](https://www.bloomberg.com/professional/) - Information density
- [Vercel Dashboard](https://vercel.com/) - Modern web app patterns
