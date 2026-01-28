import { Injectable, Logger } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { NewsArticle, NewsArticleDocument } from './news-article.schema';
import { NewsArticle as NewsArticleInterface } from '../types';

@Injectable()
export class NewsDatabaseService {
  private readonly logger = new Logger(NewsDatabaseService.name);

  constructor(
    @InjectModel(NewsArticle.name)
    private newsArticleModel: Model<NewsArticleDocument>,
  ) {}

  async saveArticle(
    article: NewsArticleInterface,
  ): Promise<NewsArticleDocument> {
    try {
      this.logger.log(
        `Start saving new article to database url=${article.url}`,
      );
      const newArticle = new this.newsArticleModel(article);
      const saved = await newArticle.save();
      this.logger.log(
        `Finish saving new article to database url=${article.url}`,
      );
      return saved;
    } catch (error) {
      this.logger.error(
        `Failed to save article ${article.url}: ${error.message}`,
      );
      throw error;
    }
  }
}
