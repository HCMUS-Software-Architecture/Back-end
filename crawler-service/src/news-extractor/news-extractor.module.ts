import { Module } from '@nestjs/common';
import { NewsExtractorService } from './news-extractor.service';
import { CoindeskNewsExtractorService } from './coindesk-news-extractor.service';
import { CointelegraphNewsExtractorService } from './cointelegraph-news-extractor.service';
import { AINewsExtractorService } from './ai-news-extractor.service';

@Module({
  providers: [
    NewsExtractorService,
    AINewsExtractorService,
    CoindeskNewsExtractorService,
    CointelegraphNewsExtractorService,
  ],
  exports: [
    NewsExtractorService,
    AINewsExtractorService,
    CoindeskNewsExtractorService,
    CointelegraphNewsExtractorService,
  ],
})
export class NewsExtractorModule {}
