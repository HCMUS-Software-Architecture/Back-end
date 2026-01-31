import { Injectable, Logger } from '@nestjs/common';
import { chromium, Browser, Page } from 'playwright';
import { NewsArticle } from 'src/database/news-article.schema';
import { INewsExtractorService } from 'src/types';
import { AINewsExtractorService } from './ai-news-extractor.service';

@Injectable()
export class CoindeskNewsExtractorService implements INewsExtractorService {
  private readonly logger = new Logger(CoindeskNewsExtractorService.name);

  constructor(private readonly aiNewsExtractor: AINewsExtractorService) {}

  async extractNews(url: string): Promise<NewsArticle> {
    let browser: Browser | null = null;
    let htmlContent = '';

    try {
      this.logger.log(`Crawling article: ${url}`);

      browser = await chromium.launch({ headless: true });
      const context = await browser.newContext({
        userAgent:
          'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
      });
      const page: Page = await context.newPage();

      await page.goto(url, {
        waitUntil: 'domcontentloaded',
        timeout: 30000,
      });

      htmlContent = await page.content();

      await page.waitForSelector('h1.font-headline-lg', {
        timeout: 10000,
      });

      const article = await page.evaluate((pageUrl: string) => {
        const headerElement = document.querySelector('h1.font-headline-lg');
        const header = headerElement?.textContent?.trim() || '';

        const subheaderElement = document.querySelector(
          '[data-module-name="article-header"] h2',
        );
        const subheader = subheaderElement?.textContent?.trim() || '';

        const dateContainer = document.querySelector(
          '[data-module-name="article-header"] .flex.gap-4.text-subtle',
        );

        const thumbnailElement = document.querySelector(
          '.article-content-wrapper figure img',
        ) as HTMLImageElement | null;
        const thumbnail = thumbnailElement?.src || '';

        const contentElement = document.querySelector(
          '[data-module-name="article-body"] .document-body',
        );
        const paragraphs = contentElement?.querySelectorAll('p') || [];
        const contentParts: string[] = [];
        paragraphs.forEach((p) => {
          const text = p.textContent?.trim();
          if (text) {
            contentParts.push(text);
          }
        });
        const content = contentParts.join('\n\n');

        return {
          header,
          subheader,
          thumbnail,
          content,
          url: pageUrl,
        };
      }, url);

      this.logger.log(`Successfully extracted article: ${article.header}`);
      return article;
    } catch (error) {
      this.logger.error(
        `Failed to extract news from ${url} using DOM-based extraction: ${error.message}`,
      );

      if (htmlContent) {
        this.logger.log('Attempting fallback extraction using Gemini AI...');

        try {
          const fallbackArticle =
            await this.aiNewsExtractor.extractNewsFromHtml(
              htmlContent,
              url,
              'Coindesk',
            );

          if (fallbackArticle.header) {
            this.logger.log(
              `Successfully extracted article "${fallbackArticle.header}" using Gemini fallback`,
            );
            return fallbackArticle;
          }
        } catch (fallbackError) {
          this.logger.error(
            `Fallback Gemini extraction also failed: ${fallbackError.message}`,
          );
        }
      }

      this.logger.error(
        'Both DOM-based and Gemini extraction failed for Coindesk',
      );
      throw error;
    } finally {
      if (browser) {
        await browser.close();
      }
    }
  }
}
