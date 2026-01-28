import { Injectable, Logger } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { NewsArticle, NewsArticleDocument } from './news-article.schema';

@Injectable()
export class NewsRepository {
  private readonly logger = new Logger(NewsRepository.name);

  constructor(
    @InjectModel(NewsArticle.name)
    private newsArticleModel: Model<NewsArticleDocument>,
  ) {}

  async findAll(
    page: number,
    limit: number,
  ): Promise<{ data: NewsArticleDocument[]; total: number }> {
    const skip = (page - 1) * limit;

    this.logger.log(
      `Start querying database: page=${page}, limit=${limit}, skip=${skip}`,
    );

    const [data, total] = await Promise.all([
      this.newsArticleModel
        .find()
        .sort({ createdAt: -1 })
        .limit(limit)
        .skip(skip)
        .exec(),
      this.newsArticleModel.countDocuments().exec(),
    ]);

    this.logger.log(
      `Finished querying database: retrieved ${data.length} articles out of ${total} total`,
    );

    return { data, total };
  }

  async findById(id: string): Promise<NewsArticleDocument | null> {
    this.logger.log(`Start querying database for ID: ${id}`);
    const article = await this.newsArticleModel.findById(id).exec();
    this.logger.log(`Finished querying database for ID: ${id}`);
    return article;
  }
}
