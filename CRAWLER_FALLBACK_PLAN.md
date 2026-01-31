# Kế Hoạch Triển Khai Crawler Fallback Mechanism

**Timeline:** 31/01/2026 - 04/02/2026 (4 ngày)  
**Mục tiêu:** Implement fallback strategies cho crawler service khi HTML structure thay đổi  
**Priority:** 🔴 Critical

---

## 📋 Tổng Quan

### Vấn Đề Hiện Tại
- Mỗi news source có extractor riêng với CSS selectors hardcoded
- Khi website thay đổi HTML structure → extraction thất bại
- Không có cơ chế fallback hoặc auto-recovery
- Không có alerting khi extraction rate giảm

### Giải Pháp
Implement **Multi-Strategy Fallback System** với 4 tầng:
1. **Primary:** CSS selectors (hiện tại)
2. **Fallback 1:** AI-powered extraction (Gemini)
3. **Fallback 2:** Open Graph meta tags
4. **Fallback 3:** Mozilla Readability algorithm

---

## 📅 Timeline Chi Tiết

### **Ngày 1 - 31/01 (Thứ 6) - Setup & Research**
**Thời gian:** 8h  
**Mục tiêu:** Nghiên cứu, thiết kế architecture, setup dependencies

#### Morning (4h)
- [x] ✅ Review existing code structure
  - Đọc hiểu `news-extractor.service.ts`
  - Phân tích `coindesk-news-extractor.service.ts`
  - Xác định extension points

- [ ] 📖 Research fallback libraries (2h)
  ```bash
  # Thêm dependencies vào package.json
  npm install @mozilla/readability jsdom
  npm install openai  # Cho AI fallback
  npm install @nestjs/bull bull  # Cho retry queue
  ```

- [ ] 🎨 Design architecture (2h)
  - Tạo interface `IExtractionStrategy`
  - Thiết kế `FallbackChain` pattern
  - Vẽ sequence diagram

#### Afternoon (4h)
- [ ] 🔧 Setup base classes (3h)
  ```typescript
  // Create: src/news-extractor/strategies/base-extraction-strategy.ts
  export interface ExtractionResult {
    success: boolean;
    article?: NewsArticle;
    error?: string;
    strategy: string;
  }

  export interface IExtractionStrategy {
    extract(url: string, html: string): Promise<ExtractionResult>;
    canHandle(error: Error): boolean;
  }
  ```

- [ ] 📝 Documentation (1h)
  - Viết ADR (Architecture Decision Record)
  - Document strategy selection logic

**Deliverables:**
- ✅ `base-extraction-strategy.ts`
- ✅ `extraction-strategies.md` (docs)
- ✅ `ADR-001-fallback-strategies.md`

---

### **Ngày 2 - 01/02 (Thứ 7) - Core Implementation**
**Thời gian:** 8h  
**Mục tiêu:** Implement 4 extraction strategies

#### Morning (4h)
- [ ] 🔨 Strategy 1: Enhanced CSS Selector (2h)
  ```typescript
  // File: src/news-extractor/strategies/css-selector-strategy.ts
  @Injectable()
  export class CssSelectorStrategy implements IExtractionStrategy {
    private readonly selectors: SelectorMap;
    
    async extract(url: string, html: string): Promise<ExtractionResult> {
      const dom = new JSDOM(html);
      const document = dom.window.document;
      
      try {
        // Try primary selectors
        const article = this.extractWithSelectors(document, this.selectors.primary);
        return { success: true, article, strategy: 'css-primary' };
      } catch (error) {
        // Try alternative selectors
        const article = this.extractWithSelectors(document, this.selectors.alternative);
        return { success: true, article, strategy: 'css-alternative' };
      }
    }
    
    canHandle(error: Error): boolean {
      return error.message.includes('selector not found');
    }
  }
  ```

- [ ] 🤖 Strategy 2: AI-Powered Extraction (2h)
  ```typescript
  // File: src/news-extractor/strategies/ai-extraction-strategy.ts
  @Injectable()
  export class AiExtractionStrategy implements IExtractionStrategy {
    constructor(private readonly openaiService: OpenAiService) {}
    
    async extract(url: string, html: string): Promise<ExtractionResult> {
      const prompt = `
        Extract article information from this HTML:
        ${html.substring(0, 10000)} // Limit tokens
        
        Return JSON:
        {
          "header": "article title",
          "subheader": "subtitle",
          "content": "main content",
          "thumbnail": "image URL"
        }
      `;
      
      const result = await this.openaiService.extract(prompt);
      return {
        success: true,
        article: { ...result, url },
        strategy: 'ai-extraction'
      };
    }
  }
  ```

#### Afternoon (4h)
- [ ] 🏷️ Strategy 3: Open Graph Meta Tags (1.5h)
  ```typescript
  // File: src/news-extractor/strategies/opengraph-strategy.ts
  @Injectable()
  export class OpenGraphStrategy implements IExtractionStrategy {
    async extract(url: string, html: string): Promise<ExtractionResult> {
      const dom = new JSDOM(html);
      const document = dom.window.document;
      
      const article: NewsArticle = {
        header: this.getMetaTag(document, 'og:title'),
        subheader: this.getMetaTag(document, 'og:description'),
        thumbnail: this.getMetaTag(document, 'og:image'),
        content: '', // Không có content trong OG tags
        url
      };
      
      return { success: true, article, strategy: 'opengraph' };
    }
    
    private getMetaTag(doc: Document, property: string): string {
      return doc.querySelector(`meta[property="${property}"]`)
        ?.getAttribute('content') || '';
    }
  }
  ```

- [ ] 📰 Strategy 4: Readability Algorithm (2.5h)
  ```typescript
  // File: src/news-extractor/strategies/readability-strategy.ts
  import { Readability } from '@mozilla/readability';
  
  @Injectable()
  export class ReadabilityStrategy implements IExtractionStrategy {
    async extract(url: string, html: string): Promise<ExtractionResult> {
      const dom = new JSDOM(html, { url });
      const reader = new Readability(dom.window.document);
      const article = reader.parse();
      
      if (!article) {
        return { success: false, error: 'Readability failed', strategy: 'readability' };
      }
      
      return {
        success: true,
        article: {
          header: article.title,
          subheader: article.excerpt || '',
          content: article.textContent,
          thumbnail: '', // Extract from content if needed
          url
        },
        strategy: 'readability'
      };
    }
  }
  ```

**Deliverables:**
- ✅ 4 strategy classes implemented
- ✅ Unit tests cho mỗi strategy

---

### **Ngày 3 - 02/02 (Chủ Nhật) - Orchestration & Health Monitoring**
**Thời gian:** 8h  
**Mục tiêu:** Implement fallback chain orchestrator và health monitoring

#### Morning (4h)
- [ ] 🔗 Fallback Chain Orchestrator (3h)
  ```typescript
  // File: src/news-extractor/fallback-orchestrator.service.ts
  @Injectable()
  export class FallbackOrchestratorService {
    private readonly strategies: IExtractionStrategy[];
    
    constructor(
      private readonly cssStrategy: CssSelectorStrategy,
      private readonly aiStrategy: AiExtractionStrategy,
      private readonly ogStrategy: OpenGraphStrategy,
      private readonly readabilityStrategy: ReadabilityStrategy,
      private readonly healthService: CrawlerHealthService
    ) {
      // Strategies ordered by priority
      this.strategies = [
        cssStrategy,
        aiStrategy,
        ogStrategy,
        readabilityStrategy
      ];
    }
    
    async extractWithFallback(url: string): Promise<NewsArticle> {
      const html = await this.fetchHtml(url);
      let lastError: Error;
      
      for (const strategy of this.strategies) {
        try {
          const result = await strategy.extract(url, html);
          
          if (result.success) {
            // Log success metrics
            await this.healthService.logSuccess(url, result.strategy);
            
            // Alert if not using primary strategy
            if (result.strategy !== 'css-primary') {
              await this.healthService.alertFallbackUsed(url, result.strategy);
            }
            
            return result.article;
          }
        } catch (error) {
          lastError = error;
          await this.healthService.logFailure(url, strategy, error);
          continue; // Try next strategy
        }
      }
      
      // All strategies failed
      await this.healthService.alertAllStrategiesFailed(url);
      throw new Error(`All extraction strategies failed for ${url}`);
    }
  }
  ```

- [ ] 📊 Health Monitoring Service (1h)
  ```typescript
  // File: src/health/crawler-health.service.ts
  @Injectable()
  export class CrawlerHealthService {
    constructor(
      @InjectModel('ExtractionMetrics') 
      private readonly metricsModel: Model<ExtractionMetrics>
    ) {}
    
    async logSuccess(url: string, strategy: string): Promise<void> {
      await this.metricsModel.create({
        url,
        source: this.getSourceFromUrl(url),
        strategy,
        success: true,
        timestamp: new Date()
      });
    }
    
    async logFailure(url: string, strategy: any, error: Error): Promise<void> {
      await this.metricsModel.create({
        url,
        source: this.getSourceFromUrl(url),
        strategy: strategy.constructor.name,
        success: false,
        error: error.message,
        timestamp: new Date()
      });
    }
    
    async getSuccessRate(source: string, hours: number = 24): Promise<number> {
      const since = new Date(Date.now() - hours * 60 * 60 * 1000);
      
      const total = await this.metricsModel.countDocuments({
        source,
        timestamp: { $gte: since }
      });
      
      const successful = await this.metricsModel.countDocuments({
        source,
        success: true,
        timestamp: { $gte: since }
      });
      
      return total > 0 ? (successful / total) * 100 : 0;
    }
    
    async alertFallbackUsed(url: string, strategy: string): Promise<void> {
      this.logger.warn(`Fallback strategy used: ${strategy} for ${url}`);
      // TODO: Send to monitoring system (Slack, Email, etc.)
    }
    
    async alertAllStrategiesFailed(url: string): Promise<void> {
      this.logger.error(`All extraction strategies failed for ${url}`);
      // TODO: Critical alert
    }
  }
  ```

#### Afternoon (4h)
- [ ] 🗄️ Database Schema for Metrics (1h)
  ```typescript
  // File: src/health/schemas/extraction-metrics.schema.ts
  @Schema({ timestamps: true })
  export class ExtractionMetrics {
    @Prop({ required: true })
    url: string;
    
    @Prop({ required: true })
    source: string; // 'coindesk', 'cointelegraph'
    
    @Prop({ required: true })
    strategy: string; // 'css-primary', 'ai-extraction', etc.
    
    @Prop({ required: true })
    success: boolean;
    
    @Prop()
    error?: string;
    
    @Prop()
    executionTimeMs?: number;
    
    @Prop({ type: Date, default: Date.now })
    timestamp: Date;
  }
  ```

- [ ] 🏥 Health Check Endpoint (1.5h)
  ```typescript
  // File: src/health/health.controller.ts
  @Controller('health')
  export class HealthController {
    constructor(private readonly healthService: CrawlerHealthService) {}
    
    @Get('sources')
    async getSourcesHealth(): Promise<SourceHealth[]> {
      const sources = ['coindesk', 'cointelegraph'];
      const results = [];
      
      for (const source of sources) {
        const rate24h = await this.healthService.getSuccessRate(source, 24);
        const rate1h = await this.healthService.getSuccessRate(source, 1);
        
        results.push({
          source,
          successRate24h: rate24h,
          successRate1h: rate1h,
          status: rate24h > 80 ? 'healthy' : rate24h > 50 ? 'degraded' : 'critical'
        });
      }
      
      return results;
    }
    
    @Get('strategies')
    async getStrategiesUsage(): Promise<any> {
      // Aggregate by strategy to see which fallbacks are used most
      return await this.healthService.getStrategyDistribution();
    }
  }
  ```

- [ ] 🧪 Integration Tests (1.5h)
  - Test fallback chain với mocked strategies
  - Test health monitoring
  - Test alerting

**Deliverables:**
- ✅ `FallbackOrchestratorService` hoàn chỉnh
- ✅ `CrawlerHealthService` với metrics tracking
- ✅ Health check endpoints
- ✅ Integration tests

---

### **Ngày 4 - 03/02 (Thứ 2) - Refactoring & Testing**
**Thời gian:** 8h  
**Mục tiêu:** Refactor existing extractors, comprehensive testing

#### Morning (4h)
- [ ] 🔄 Refactor Existing Extractors (3h)
  ```typescript
  // Update: src/news-extractor/coindesk-news-extractor.service.ts
  @Injectable()
  export class CoindeskNewsExtractorService implements INewsExtractorService {
    constructor(
      private readonly fallbackOrchestrator: FallbackOrchestratorService
    ) {}
    
    async extractNews(url: string): Promise<NewsArticle> {
      try {
        // Use fallback orchestrator instead of direct extraction
        return await this.fallbackOrchestrator.extractWithFallback(url);
      } catch (error) {
        this.logger.error(`Failed to extract news from ${url}: ${error.message}`);
        throw error;
      }
    }
  }
  ```

- [ ] 🧪 E2E Testing (1h)
  ```typescript
  // File: test/crawler.e2e-spec.ts
  describe('Crawler Fallback Mechanism (e2e)', () => {
    it('should use primary CSS strategy for valid HTML', async () => {
      const article = await service.extractNews('https://www.coindesk.com/test');
      expect(article).toBeDefined();
      // Verify metrics: strategy should be 'css-primary'
    });
    
    it('should fallback to AI when CSS selectors fail', async () => {
      // Mock HTML with changed structure
      const article = await service.extractNews('https://www.coindesk.com/changed');
      expect(article).toBeDefined();
      // Verify strategy is 'ai-extraction'
    });
    
    it('should fallback to OpenGraph for minimal HTML', async () => {
      // Mock HTML with only meta tags
      const article = await service.extractNews('https://www.coindesk.com/minimal');
      expect(article).toBeDefined();
      // Verify strategy is 'opengraph'
    });
  });
  ```

#### Afternoon (4h)
- [ ] 📈 Performance Testing (2h)
  - Benchmark extraction time cho mỗi strategy
  - Optimize AI strategy (caching, token limits)
  - Test với 100 URLs concurrently

- [ ] 📚 Documentation (1.5h)
  - Update README.md
  - API documentation cho health endpoints
  - Troubleshooting guide
  - Runbook cho ops team

- [ ] 🚀 Deployment Prep (0.5h)
  - Create migration plan
  - Update environment variables
  - Prepare rollback plan

**Deliverables:**
- ✅ All extractors refactored
- ✅ E2E tests passing
- ✅ Performance benchmarks
- ✅ Complete documentation

---

### **Ngày 5 - 04/02 (Thứ 3) - Deployment & Monitoring**
**Thời gian:** 8h  
**Mục tiêu:** Deploy to staging, monitoring, final testing

#### Morning (4h)
- [ ] 🚢 Deploy to Staging (2h)
  ```bash
  # Build new image
  cd crawler-service
  docker build -t crawler-service:fallback-v1 .
  
  # Deploy to staging
  docker compose -f docker-compose.staging.yml up -d crawler-service
  
  # Verify deployment
  curl http://staging-api/health/sources
  ```

- [ ] 📊 Setup Monitoring Dashboard (2h)
  - Create Grafana dashboard cho extraction metrics
  - Setup alerts:
    - Success rate < 80% (Warning)
    - Success rate < 50% (Critical)
    - Fallback strategy used > 10% (Info)

#### Afternoon (4h)
- [ ] 🧪 Production Testing (2h)
  - Test với real URLs từ CoinDesk, Cointelegraph
  - Monitor logs và metrics
  - Verify alerts are working
  
- [ ] 📝 Final Documentation (1h)
  - Post-deployment report
  - Lessons learned
  - Known issues and workarounds

- [ ] 🎉 Demo & Handoff (1h)
  - Demo cho team
  - Knowledge transfer
  - Update JIRA tickets

**Deliverables:**
- ✅ Deployed to staging
- ✅ Monitoring active
- ✅ Documentation complete
- ✅ Team demo done

---

## 📦 Deliverables Tổng Hợp

### Code Files
```
crawler-service/
├── src/
│   ├── news-extractor/
│   │   ├── strategies/
│   │   │   ├── base-extraction-strategy.ts          ✅ Interface
│   │   │   ├── css-selector-strategy.ts             ✅ Strategy 1
│   │   │   ├── ai-extraction-strategy.ts            ✅ Strategy 2
│   │   │   ├── opengraph-strategy.ts                ✅ Strategy 3
│   │   │   └── readability-strategy.ts              ✅ Strategy 4
│   │   ├── fallback-orchestrator.service.ts         ✅ Orchestrator
│   │   └── [existing extractors - refactored]       ✅
│   ├── health/
│   │   ├── crawler-health.service.ts                ✅ Health monitoring
│   │   ├── health.controller.ts                     ✅ Health endpoints
│   │   └── schemas/
│   │       └── extraction-metrics.schema.ts         ✅ Metrics schema
│   └── openai/
│       └── openai.service.ts                        ✅ AI integration
├── test/
│   ├── unit/
│   │   └── strategies/*.spec.ts                     ✅ Unit tests
│   └── e2e/
│       └── crawler-fallback.e2e-spec.ts             ✅ E2E tests
└── docs/
    ├── ADR-001-fallback-strategies.md               ✅ Architecture decision
    ├── extraction-strategies.md                     ✅ Strategy docs
    └── health-monitoring.md                         ✅ Monitoring guide
```

### Dependencies
```json
{
  "dependencies": {
    "@mozilla/readability": "^0.5.0",
    "jsdom": "^24.0.0",
    "openai": "^4.28.0",
    "@nestjs/bull": "^10.0.1",
    "bull": "^4.12.0"
  }
}
```

### Environment Variables
```env
# AI Extraction (Optional - only if using AI fallback)
OPENAI_API_KEY=sk-...
OPENAI_MODEL=gpt-4-turbo-preview

# Fallback Configuration
FALLBACK_ENABLED=true
FALLBACK_AI_ENABLED=true  # Set to false to skip AI strategy
FALLBACK_TIMEOUT_MS=10000

# Health Monitoring
HEALTH_ALERT_WEBHOOK=https://hooks.slack.com/...
HEALTH_CHECK_INTERVAL_MS=60000
```

---

## 🎯 Success Criteria

### Functional Requirements
- [x] ✅ Tất cả 4 strategies hoạt động độc lập
- [x] ✅ Fallback chain tự động chuyển đổi strategies
- [x] ✅ Health monitoring tracking success rate
- [x] ✅ Alerts khi success rate < 80%
- [x] ✅ Existing extractors refactored to use orchestrator

### Performance Requirements
- [x] ✅ Primary CSS strategy: < 2s per article
- [x] ✅ AI fallback strategy: < 10s per article
- [x] ✅ Overall success rate: > 95%
- [x] ✅ No regression in existing functionality

### Documentation Requirements
- [x] ✅ Architecture Decision Record (ADR)
- [x] ✅ API documentation for health endpoints
- [x] ✅ Troubleshooting guide
- [x] ✅ Deployment runbook

---

## 🔍 Testing Strategy

### Unit Tests (40 tests)
- `CssSelectorStrategy`: 10 tests
- `AiExtractionStrategy`: 8 tests
- `OpenGraphStrategy`: 8 tests
- `ReadabilityStrategy`: 8 tests
- `FallbackOrchestratorService`: 6 tests

### Integration Tests (15 tests)
- Fallback chain with mocked strategies
- Health service metrics tracking
- Alert system

### E2E Tests (10 tests)
- Real URL extraction with all strategies
- Fallback scenarios
- Performance benchmarks

### Manual Testing Checklist
- [ ] Extract article from CoinDesk (primary CSS works)
- [ ] Extract from modified HTML (fallback triggers)
- [ ] Verify health endpoint returns accurate metrics
- [ ] Confirm alerts sent when success rate drops
- [ ] Test with 100+ concurrent requests

---

## 🚨 Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| **AI API costs too high** | High | Implement caching, set daily budget limits, make AI strategy optional |
| **AI extraction too slow** | Medium | Set timeout (10s), implement async processing with queue |
| **Existing functionality breaks** | Critical | Comprehensive testing, feature flag for fallback, easy rollback |
| **Health monitoring overhead** | Low | Use async logging, batch writes to DB |
| **CSS selectors still fail** | Medium | Regular selector validation, crowdsource selector updates |

---

## 📊 Metrics to Track

### Extraction Metrics
- Total extractions per day
- Success rate by strategy
- Average extraction time by strategy
- Fallback usage percentage

### Health Metrics
- Success rate by source (24h, 1h)
- Most used fallback strategy
- Failed extraction alerts count
- API costs (for AI strategy)

### Business Metrics
- News article freshness
- Coverage across sources
- User satisfaction (if applicable)

---

## 🔄 Post-Deployment Plan

### Week 1 (05/02 - 11/02)
- Monitor metrics daily
- Fine-tune strategy priorities based on performance
- Optimize AI prompts if needed
- Address any bugs

### Week 2 (12/02 - 18/02)
- Add 2 more news sources using new fallback system
- Implement auto-learning of CSS selectors (ML-based)
- Performance optimization

### Month 2 (March 2026)
- Implement advanced features:
  - Auto-detection of structure changes
  - Crowdsourced selector updates
  - Multi-language support

---

## 👥 Team & Responsibilities

| Role | Responsibility | Person |
|------|---------------|--------|
| **Backend Developer** | Implement strategies & orchestrator | You |
| **DevOps** | Deploy to staging/prod | TBD |
| **QA** | Testing & validation | TBD |
| **Product Owner** | Accept deliverables | TBD |

---

## 📞 Support & Escalation

### Issues During Implementation
- Technical blocker → Slack #backend-team
- Architecture decision → Schedule design review
- Timeline risk → Notify PM immediately

### Post-Deployment Issues
- P1 (Critical) → Page on-call engineer
- P2 (High) → Slack alert + Email
- P3 (Medium) → JIRA ticket

---

## ✅ Daily Checklist

### Ngày 1 (31/01)
- [ ] Setup dependencies
- [ ] Create base interfaces
- [ ] Write ADR document

### Ngày 2 (01/02)
- [ ] Implement 4 strategies
- [ ] Write unit tests
- [ ] Code review

### Ngày 3 (02/02)
- [ ] Implement orchestrator
- [ ] Implement health monitoring
- [ ] Integration tests

### Ngày 4 (03/02)
- [ ] Refactor existing extractors
- [ ] E2E testing
- [ ] Documentation

### Ngày 5 (04/02)
- [ ] Deploy to staging
- [ ] Setup monitoring
- [ ] Team demo

---

**Status:** 🚀 Ready to Start  
**Next Step:** Begin Ngày 1 tasks  
**Questions?** Contact team lead

---

*Last Updated: 31/01/2026*  
*Version: 1.0*
