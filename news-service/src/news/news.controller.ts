import {
  Controller,
  Get,
  Param,
  Query,
  NotFoundException,
  BadRequestException,
  Logger,
} from '@nestjs/common';
import {
  ApiTags,
  ApiOperation,
  ApiResponse,
  ApiParam,
  ApiQuery,
} from '@nestjs/swagger';
import { NewsService } from './news.service';
import {
  NewsArticleDto,
  PaginatedNewsResponseDto,
} from './dto/news-article.dto';

@ApiTags('news')
@Controller('api/news')
export class NewsController {
  private readonly logger = new Logger(NewsController.name);

  constructor(private readonly newsService: NewsService) {}

  @Get()
  @ApiOperation({
    summary: 'Get paginated news articles',
    description:
      'Retrieves a paginated list of news articles sorted by creation date (newest first)',
  })
  @ApiQuery({
    name: 'page',
    required: false,
    type: Number,
    description: 'Page number (default: 1)',
    example: 1,
  })
  @ApiQuery({
    name: 'limit',
    required: false,
    type: Number,
    description: 'Number of items per page (default: 10, max: 100)',
    example: 10,
  })
  @ApiResponse({
    status: 200,
    description: 'Successfully retrieved news articles',
    type: PaginatedNewsResponseDto,
  })
  @ApiResponse({
    status: 400,
    description: 'Invalid query parameters',
  })
  async getNews(
    @Query('page') page: string = '1',
    @Query('limit') limit: string = '10',
  ): Promise<PaginatedNewsResponseDto> {
    const pageNum = parseInt(page, 10);
    const limitNum = parseInt(limit, 10);

    if (isNaN(pageNum) || pageNum < 1) {
      throw new BadRequestException('Page must be a positive number');
    }

    if (isNaN(limitNum) || limitNum < 1 || limitNum > 100) {
      throw new BadRequestException('Limit must be between 1 and 100');
    }

    const { data, total } = await this.newsService.findAll(pageNum, limitNum);

    const totalPages = Math.ceil(total / limitNum);

    return {
      data: data.map((article) => article.toObject()),
      total,
      page: pageNum,
      limit: limitNum,
      totalPages,
    };
  }

  @Get(':newsId')
  @ApiOperation({
    summary: 'Get a specific news article by ID',
    description: 'Retrieves detailed information about a single news article',
  })
  @ApiParam({
    name: 'newsId',
    description: 'MongoDB ObjectId of the news article',
    example: '507f1f77bcf86cd799439011',
  })
  @ApiResponse({
    status: 200,
    description: 'Successfully retrieved news article',
    type: NewsArticleDto,
  })
  @ApiResponse({
    status: 404,
    description: 'News article not found',
  })
  @ApiResponse({
    status: 400,
    description: 'Invalid news ID format',
  })
  async getNewsById(@Param('newsId') newsId: string): Promise<NewsArticleDto> {
    if (!newsId.match(/^[0-9a-fA-F]{24}$/)) {
      throw new BadRequestException('Invalid news ID format');
    }

    const article = await this.newsService.findById(newsId);

    if (!article) {
      throw new NotFoundException(`News article with ID ${newsId} not found`);
    }

    return article.toObject();
  }
}
