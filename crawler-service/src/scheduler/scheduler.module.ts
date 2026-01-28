import { Module } from '@nestjs/common';
import { CrawlerSchedulerService } from './crawler-scheduler.service';
import { UrlExtractorModule } from '../url-extractor/url-extractor.module';

@Module({
  imports: [UrlExtractorModule],
  providers: [CrawlerSchedulerService],
  exports: [CrawlerSchedulerService],
})
export class SchedulerModule {}
