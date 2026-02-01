import { Injectable, Logger, Inject } from '@nestjs/common';
import { CACHE_MANAGER } from '@nestjs/cache-manager';
import type { Cache } from 'cache-manager';
import { NewsArticleDocument } from './news-article.schema';
import { NewsRepository } from './news.repository';

@Injectable()
export class NewsService {
  private readonly logger = new Logger(NewsService.name);
  private readonly ALL_NEWS_TTL = 300; 
  private readonly NEWS_BY_ID_TTL = 86400;

  constructor(
    private readonly newsRepository: NewsRepository,
    @Inject(CACHE_MANAGER) private cacheManager: Cache,
  ) {}

  async findAll(
    page: number = 1,
    limit: number = 10,
  ): Promise<{ data: NewsArticleDocument[]; total: number }> {
    const cacheKey = `news:all:page:${page}:limit:${limit}`;

    const cachedResult = await this.cacheManager.get<{
      data: NewsArticleDocument[];
      total: number;
    }>(cacheKey);

    if (cachedResult) {
      this.logger.log(
        `Cache hit for news: page=${page}, limit=${limit}, retrieved ${cachedResult.data.length} articles from cache`,
      );
      return cachedResult;
    }

    this.logger.log(`Cache miss - Fetching news from DB: page=${page}, limit=${limit}`);
    const result = await this.newsRepository.findAll(page, limit);

    await this.cacheManager.set(cacheKey, result, this.ALL_NEWS_TTL * 1000);

    this.logger.log(
      `Finish fetching news: retrieved ${result.data.length} articles out of ${result.total} total (stored in cache)`,
    );
    return result;
  }

  async findById(id: string): Promise<NewsArticleDocument | null> {
    const cacheKey = `news:id:${id}`;

    const cachedArticle =
      await this.cacheManager.get<NewsArticleDocument>(cacheKey);

    if (cachedArticle) {
      this.logger.log(`Cache hit for news ID: ${id}`);
      return cachedArticle;
    }

    this.logger.log(`Cache miss - Fetching news by ID from DB: ${id}`);
    const article = await this.newsRepository.findById(id);

    if (article) {
      await this.cacheManager.set(cacheKey, article, this.NEWS_BY_ID_TTL * 1000);
      this.logger.log(`News article ${id} stored in cache`);
      return article;
    }
    else {
      throw new Error(`News article with ID ${id} not found`);
    }
  }
}
