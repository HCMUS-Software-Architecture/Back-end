import { ApiProperty } from '@nestjs/swagger';

export class NewsArticleDto {
  @ApiProperty({
    example: '507f1f77bcf86cd799439011',
    description: 'Article ID',
  })
  _id: string;

  @ApiProperty({ example: 'Bitcoin Reaches New All-Time High' })
  header: string;

  @ApiProperty({
    example: 'Bitcoin surpasses $100,000 mark for the first time in history',
  })
  subheader: string;

  @ApiProperty({ example: 'https://example.com/image.jpg' })
  thumbnail: string;

  @ApiProperty({ example: 'Full article content here...' })
  content: string;

  @ApiProperty({ example: 'https://example.com/article' })
  url: string;

  @ApiProperty({ example: '2026-01-28T10:00:00.000Z' })
  createdAt: Date;

  @ApiProperty({ example: '2026-01-28T10:00:00.000Z' })
  updatedAt: Date;
}

export class PaginatedNewsResponseDto {
  @ApiProperty({
    type: [NewsArticleDto],
    description: 'Array of news articles',
  })
  data: NewsArticleDto[];

  @ApiProperty({ example: 100, description: 'Total number of articles' })
  total: number;

  @ApiProperty({ example: 1, description: 'Current page number' })
  page: number;

  @ApiProperty({ example: 10, description: 'Items per page' })
  limit: number;

  @ApiProperty({ example: 10, description: 'Total number of pages' })
  totalPages: number;
}
