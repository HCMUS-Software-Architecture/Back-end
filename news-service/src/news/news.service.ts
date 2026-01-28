import { Injectable, Logger } from '@nestjs/common';
import { NewsArticleDocument } from './news-article.schema';
import { NewsRepository } from './news.repository';

@Injectable()
export class NewsService {
  private readonly logger = new Logger(NewsService.name);

  constructor(private readonly newsRepository: NewsRepository) {}

  async findAll(
    page: number = 1,
    limit: number = 10,
  ): Promise<{ data: NewsArticleDocument[]; total: number }> {
    this.logger.log(`Start fetching news: page=${page}, limit=${limit}`);
    const result = await this.newsRepository.findAll(page, limit);
    this.logger.log(
      `Finish fetching news: retrieved ${result.data.length} articles out of ${result.total} total`,
    );
    return result;
  }

  async findById(id: string): Promise<NewsArticleDocument | null> {
    this.logger.log(`Start fetching news by ID: ${id}`);
    const article = await this.newsRepository.findById(id);
    return article;
  }
}
