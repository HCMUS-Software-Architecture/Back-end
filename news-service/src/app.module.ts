import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { NewsModule } from './news/news.module';

@Module({
  imports: [MongooseModule.forRoot(process.env.MONGODB_URI!), NewsModule],
})
export class AppModule {}
