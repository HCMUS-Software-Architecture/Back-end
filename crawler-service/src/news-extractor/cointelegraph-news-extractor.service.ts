import { Injectable, Logger } from '@nestjs/common';
import { chromium, Browser, Page } from 'playwright';
import { INewsExtractorService } from 'src/types';
import { NewsArticle } from 'src/database/news-article.schema';

@Injectable()
export class CointelegraphNewsExtractorService implements INewsExtractorService {
  private readonly logger = new Logger(CointelegraphNewsExtractorService.name);

  async extractNews(url: string): Promise<NewsArticle> {
    let browser: Browser | null = null;

    try {
      this.logger.log(`Start crawling article: ${url}`);

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

      await page.waitForSelector('article h1', {
        timeout: 15000,
      });

      const article = await page.evaluate((pageUrl: string) => {
        const headerElement = document.querySelector(
          '[data-testid="post-title"]',
        );
        const header = headerElement?.textContent?.trim() || '';

        const subheaderElement = document.querySelector(
          '[data-testid="post-description"]',
        );
        const subheader = subheaderElement?.textContent?.trim() || '';

        const thumbnailElement = document.querySelector(
          '[data-testid="post-cover-image"]',
        ) as HTMLImageElement | null;
        const thumbnail = thumbnailElement?.src || '';

        const contentElement = document.querySelector(
          '[data-testid="html-renderer-container"]',
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

      this.logger.log(`Finish extracting article: ${article.header}`);
      return article;
    } catch (error) {
      this.logger.error(`Failed to extract news from ${url}: ${error.message}`);
      throw error;
    } finally {
      if (browser) {
        await browser.close();
      }
    }
  }
}
