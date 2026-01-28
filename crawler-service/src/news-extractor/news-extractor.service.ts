import { Injectable, Logger } from '@nestjs/common';
import { INewsExtractorService } from 'src/types';
import { CoindeskNewsExtractorService } from './coindesk-news-extractor.service';
import { CointelegraphNewsExtractorService } from './cointelegraph-news-extractor.service';
import { NewsArticle } from 'src/database/news-article.schema';

@Injectable()
export class NewsExtractorService {
  private readonly logger = new Logger(NewsExtractorService.name);

  constructor(
    private readonly coindeskExtractor: CoindeskNewsExtractorService,
    private readonly cointelegraphExtractor: CointelegraphNewsExtractorService,
  ) {}

  getNewsExtractorService(url: string): INewsExtractorService | null {
    if (url.includes('coindesk.com')) {
      return this.coindeskExtractor;
    }

    if (url.includes('cointelegraph.com')) {
      return this.cointelegraphExtractor;
    }

    this.logger.warn(`No extractor found for URL: ${url}`);
    return null;
  }

  async extractNews(url: string): Promise<NewsArticle> {
    const extractor = this.getNewsExtractorService(url);

    if (!extractor) {
      throw new Error(`Unsupported news source for URL: ${url}`);
    }

    this.logger.log(`Extracting news from: ${url}`);
    return extractor.extractNews(url);
  }
}
