import { Module, forwardRef } from '@nestjs/common';
import { BullModule } from '@nestjs/bullmq';
import Redis from 'ioredis';
import {
  UrlExtractorService,
  URL_QUEUE_NAME,
  REDIS_CLIENT,
} from './url-extractor.service';
import { CoindeskUrlExtractorService } from './coindesk-url-extractor.service';
import { CointelegraphUrlExtractorService } from './cointelegraph-url-extractor.service';
import { AIUrlExtractorService } from './ai-url-extractor.service';
import { UrlQueueProcessor } from './url-queue.processor';
import { NewsExtractorModule } from '../news-extractor/news-extractor.module';
import { NewsDatabaseModule } from '../database/news-database.module';

const redisConfig = {
  host: process.env.REDIS_HOST!,
  port: parseInt(process.env.REDIS_PORT!),
  password: process.env.REDIS_PASSWORD!,
  tls: {},
};

@Module({
  imports: [
    BullModule.registerQueue({
      name: URL_QUEUE_NAME,
      connection: redisConfig,
    }),
    forwardRef(() => NewsExtractorModule),
    NewsDatabaseModule,
  ],
  controllers: [],
  providers: [
    {
      provide: REDIS_CLIENT,
      useFactory: () => {
        return new Redis(redisConfig);
      },
    },
    UrlExtractorService,
    AIUrlExtractorService,
    CoindeskUrlExtractorService,
    CointelegraphUrlExtractorService,
    UrlQueueProcessor,
  ],
  exports: [
    UrlExtractorService,
    AIUrlExtractorService,
    CoindeskUrlExtractorService,
    CointelegraphUrlExtractorService,
    REDIS_CLIENT,
  ],
})
export class UrlExtractorModule {}
