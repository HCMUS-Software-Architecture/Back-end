import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { NewsArticle, NewsArticleSchema } from './news-article.schema';
import { NewsDatabaseService } from './news-database.service';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: NewsArticle.name, schema: NewsArticleSchema },
    ]),
  ],
  providers: [NewsDatabaseService],
  exports: [NewsDatabaseService],
})
export class NewsDatabaseModule {}
