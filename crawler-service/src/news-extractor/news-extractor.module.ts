import { Module } from '@nestjs/common';
import { NewsExtractorService } from './news-extractor.service';
import { CoindeskNewsExtractorService } from './coindesk-news-extractor.service';
import { CointelegraphNewsExtractorService } from './cointelegraph-news-extractor.service';

@Module({
  providers: [
    NewsExtractorService,
    CoindeskNewsExtractorService,
    CointelegraphNewsExtractorService,
  ],
  exports: [
    NewsExtractorService,
    CoindeskNewsExtractorService,
    CointelegraphNewsExtractorService,
  ],
})
export class NewsExtractorModule {}
