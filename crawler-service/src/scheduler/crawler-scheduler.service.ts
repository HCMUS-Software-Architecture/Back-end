import { Injectable, Logger } from '@nestjs/common';
import { Cron } from '@nestjs/schedule';
import { UrlExtractorService } from '../url-extractor/url-extractor.service';

@Injectable()
export class CrawlerSchedulerService {
  private readonly logger = new Logger(CrawlerSchedulerService.name);

  constructor(private readonly urlExtractorService: UrlExtractorService) {}

  @Cron(process.env.CRAWLER_EVERY_5_MINUTES!)
  async handleCrawlerSchedule() {
    this.logger.log('Start scheduling crawler job');

    try {
      this.urlExtractorService.extractUrls();
      this.logger.log(`Finish scheduling crawler job`);
    } catch (error) {
      this.logger.error(
        `Scheduled crawler failed: ${error.message}`,
        error.stack,
      );
    }
  }
}
