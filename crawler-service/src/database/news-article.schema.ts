import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document } from 'mongoose';

export type NewsArticleDocument = NewsArticle & Document;

@Schema({ timestamps: true })
export class NewsArticle {
  @Prop({ required: true })
  header: string;

  @Prop({ required: true })
  subheader: string;

  @Prop({ required: true })
  thumbnail: string;

  @Prop({ required: true })
  content: string;

  @Prop({ required: true, unique: true })
  url: string;
}

export const NewsArticleSchema = SchemaFactory.createForClass(NewsArticle);
