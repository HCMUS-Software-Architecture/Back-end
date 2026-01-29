import { Processor, WorkerHost } from '@nestjs/bullmq';
import { Logger } from '@nestjs/common';
import { Job } from 'bullmq';
import { URL_QUEUE_NAME } from './url-extractor.service';
import { NewsExtractorService } from '../news-extractor/news-extractor.service';
import { NewsDatabaseService } from '../database/news-database.service';
import { RabbitMQService } from '../messaging/rabbitmq.service';
import { NewsArticle } from 'src/types';

export interface UrlJobData {
  url: string;
  timestamp: number;
}

@Processor(URL_QUEUE_NAME)
export class UrlQueueProcessor extends WorkerHost {
  private readonly logger = new Logger(UrlQueueProcessor.name);

  constructor(
    private readonly newsExtractorService: NewsExtractorService,
    private readonly newsDatabaseService: NewsDatabaseService,
    private readonly rabbitMQService: RabbitMQService,
  ) {
    super();
  }

  async process(job: Job<UrlJobData>): Promise<NewsArticle | null> {
    const { url, timestamp } = job.data;

    this.logger.log(
      `Processing job ${job.id}: ${url} (queued at ${new Date(timestamp).toISOString()})`,
    );

    try {
      const article = await this.newsExtractorService.extractNews(url);

      this.logger.log(
        `Successfully extracted article: "${article.header}" from ${url}`,
      );

      await this.newsDatabaseService.saveArticle(article);

      await this.rabbitMQService.publishNewsForAnalysis(article);
      this.logger.log(
        `Published article for sentiment analysis: "${article.header}"`,
      );

      return article;
    } catch (error) {
      this.logger.error(
        `Failed to process job ${job.id} for URL ${url}: ${error.message}`,
      );
      throw error;
    }
  }
}
