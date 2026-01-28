import * as dotenv from 'dotenv';
dotenv.config();

import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);

  app.enableCors({
    origin: '*',
    methods: 'GET,HEAD,PUT,PATCH,POST,DELETE,OPTIONS',
    credentials: true,
  });

  // Swagger Configuration
  const config = new DocumentBuilder()
    .setTitle('Crawler Service API')
    .setDescription('API for crawling crypto news from various sources')
    .setVersion('1.0')
    .addTag('crawler')
    .build();

  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup('api', app, document);

  await app.listen(parseInt(process.env.PORT!));
  console.log(
    `Application is running on: http://localhost:${process.env.PORT}`,
  );
  console.log(
    `Swagger documentation available at: http://localhost:${process.env.PORT}/api`,
  );
}
bootstrap();
