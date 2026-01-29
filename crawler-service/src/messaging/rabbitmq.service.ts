import { Injectable, Logger, OnModuleInit, OnModuleDestroy } from '@nestjs/common';
import * as amqp from 'amqplib';
import { NewsArticle } from '../types';

export const NEWS_ANALYSIS_QUEUE = 'news_analysis_queue';
export const NEWS_ANALYSIS_EXCHANGE = 'news_analysis_exchange';

export interface NewsAnalysisMessage {
  header: string;
  subheader: string;
  content: string;
  url: string;
  crawled_at: string;
}

@Injectable()
export class RabbitMQService implements OnModuleInit, OnModuleDestroy {
  private readonly logger = new Logger(RabbitMQService.name);
  private connection: amqp.ChannelModel | null = null;
  private channel: amqp.Channel | null = null;
  private readonly url = process.env.RABBITMQ_URL!;

  async onModuleInit(): Promise<void> {
    await this.connect();
  }

  async onModuleDestroy(): Promise<void> {
    await this.disconnect();
  }

  private async connect(): Promise<void> {
    try {
      this.logger.log(`Connecting to RabbitMQ at ${this.url}`);
      this.connection = await amqp.connect(this.url);
      this.channel = await this.connection.createChannel();

      await this.channel.assertExchange(NEWS_ANALYSIS_EXCHANGE, 'direct', {
        durable: true,
      });

      await this.channel.assertQueue(NEWS_ANALYSIS_QUEUE, {
        durable: true,
      });

      await this.channel.bindQueue(
        NEWS_ANALYSIS_QUEUE,
        NEWS_ANALYSIS_EXCHANGE,
        NEWS_ANALYSIS_QUEUE,
      );

      this.logger.log('Successfully connected to RabbitMQ');
    } catch (error) {
      this.logger.error(`Failed to connect to RabbitMQ: ${error.message}`);
      throw error;
    }
  }

  private async disconnect(): Promise<void> {
    try {
      if (this.channel) {
        await this.channel.close();
      }
      if (this.connection) {
        await this.connection.close();
      }
      this.logger.log('Disconnected from RabbitMQ');
    } catch (error) {
      this.logger.error(`Error disconnecting from RabbitMQ: ${error.message}`);
    }
  }

  async publishNewsForAnalysis(article: NewsArticle): Promise<void> {
    if (!this.channel) {
      throw new Error('RabbitMQ channel is not initialized');
    }

    const message: NewsAnalysisMessage = {
      header: article.header,
      subheader: article.subheader,
      content: article.content,
      url: article.url,
      crawled_at: new Date().toISOString(),
    };

    try {
      this.channel.publish(
        NEWS_ANALYSIS_EXCHANGE,
        NEWS_ANALYSIS_QUEUE,
        Buffer.from(JSON.stringify(message)),
        {
          persistent: true,
          contentType: 'application/json',
        },
      );

      this.logger.log(
        `Published news article to analysis queue: "${article.header}"`,
      );
    } catch (error) {
      this.logger.error(
        `Failed to publish news article: ${error.message}`,
      );
      throw error;
    }
  }
}
