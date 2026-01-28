import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { ScheduleModule } from '@nestjs/schedule';
import { UrlExtractorModule } from './url-extractor/url-extractor.module';
import { NewsExtractorModule } from './news-extractor/news-extractor.module';
import { NewsDatabaseModule } from './database/news-database.module';
import { SchedulerModule } from './scheduler/scheduler.module';
import { CoindeskNewsExtractorService } from './news-extractor/coindesk-news-extractor.service';
import { CointelegraphNewsExtractorService } from './news-extractor/cointelegraph-news-extractor.service';

@Module({
  imports: [
    ScheduleModule.forRoot(),
    MongooseModule.forRoot(process.env.MONGODB_URI!),
    UrlExtractorModule,
    NewsExtractorModule,
    NewsDatabaseModule,
    SchedulerModule,
  ],
  providers: [CoindeskNewsExtractorService, CointelegraphNewsExtractorService],
})
export class AppModule {}
