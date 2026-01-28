import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { NewsArticle, NewsArticleSchema } from './news-article.schema';
import { NewsService } from './news.service';
import { NewsRepository } from './news.repository';
import { NewsController } from './news.controller';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: NewsArticle.name, schema: NewsArticleSchema },
    ]),
  ],
  controllers: [NewsController],
  providers: [NewsService, NewsRepository],
  exports: [NewsService],
})
export class NewsModule {}
