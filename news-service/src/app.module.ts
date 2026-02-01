import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { CacheModule } from '@nestjs/cache-manager';
import * as redisStore from 'cache-manager-redis-store';
import { NewsModule } from './news/news.module';

@Module({
  imports: [
    MongooseModule.forRoot(process.env.MONGODB_URI!),
    CacheModule.register({
      isGlobal: true,
      store: redisStore,
      host: process.env.REDIS_HOST!,
      port: parseInt(process.env.REDIS_PORT!),
      password: process.env.REDIS_PASSWORD!,
    }),
    NewsModule,
  ],
})
export class AppModule {}
