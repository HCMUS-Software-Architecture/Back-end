import { Injectable, Logger, Inject } from '@nestjs/common';
import { InjectQueue } from '@nestjs/bullmq';
import { Queue } from 'bullmq';
import Redis from 'ioredis';
import { CoindeskUrlExtractorService } from './coindesk-url-extractor.service';
import { CointelegraphUrlExtractorService } from './cointelegraph-url-extractor.service';
import { IUrlExtractorService } from 'src/types';
import { AIUrlExtractorService } from './ai-url-extractor.service';

export const URL_QUEUE_NAME = 'url-queue';
export const REDIS_CLIENT = 'REDIS_CLIENT';

@Injectable()
export class UrlExtractorService {
  private readonly logger = new Logger(UrlExtractorService.name);
  private readonly urlExtractorServices: IUrlExtractorService[] = [];
  private readonly redisKeyPrefix = 'crawled:url:';

  constructor(
    @Inject(REDIS_CLIENT) private readonly redis: Redis,
    @InjectQueue(URL_QUEUE_NAME) private readonly urlQueue: Queue,
    private readonly aiUrlExtractorService: AIUrlExtractorService,
  ) {
    this.urlExtractorServices.push(
      new CointelegraphUrlExtractorService(this.aiUrlExtractorService),
    );
    this.urlExtractorServices.push(
      new CoindeskUrlExtractorService(this.aiUrlExtractorService),
    );
  }

  async extractUrls(): Promise<string[]> {
    this.logger.log(
      `Starting URL extraction from ${this.urlExtractorServices.length} extractor(s)`,
    );

    const allUrls: string[] = [];
    for (const extractor of this.urlExtractorServices) {
      try {
        const urls = await extractor.extractUrls();
        allUrls.push(...urls);
      } catch (error) {
        this.logger.error(
          `Failed to extract URLs from extractor: ${error.message}`,
        );
      }
    }

    const uniqueUrls = [...new Set(allUrls)];
    this.logger.log(`Extracted ${uniqueUrls.length} unique URLs`);

    const newUrls = await this.filterNewUrls(uniqueUrls);
    this.logger.log(`Found ${newUrls.length} new URLs after filtering`);

    if (newUrls.length > 0) {
      await this.pushToQueue(newUrls);
      await this.markUrlsAsCrawled(newUrls);
    }

    return newUrls;
  }

  private async filterNewUrls(urls: string[]): Promise<string[]> {
    const newUrls: string[] = [];

    const pipeline = this.redis.pipeline();
    for (const url of urls) {
      pipeline.exists(this.getRedisKey(url));
    }

    const results = await pipeline.exec();

    if (results) {
      for (let i = 0; i < urls.length; i++) {
        const [error, exists] = results[i];
        if (!error && exists === 0) {
          newUrls.push(urls[i]);
        }
      }
    }

    return newUrls;
  }

  private async pushToQueue(urls: string[]): Promise<void> {
    const jobs = urls.map((url) => ({
      name: 'crawl-url',
      data: { url, timestamp: Date.now() },
    }));

    await this.urlQueue.addBulk(jobs);
    this.logger.log(`Pushed ${urls.length} URLs to queue`);
  }

  private async markUrlsAsCrawled(urls: string[]): Promise<void> {
    const pipeline = this.redis.pipeline();
    const ttl = 60 * 60 * 24 * 7;

    for (const url of urls) {
      pipeline.set(this.getRedisKey(url), Date.now().toString(), 'EX', ttl);
    }

    await pipeline.exec();
    this.logger.log(`Marked ${urls.length} URLs as crawled in Redis`);
  }

  private getRedisKey(url: string): string {
    return `${this.redisKeyPrefix}${url}`;
  }
}
