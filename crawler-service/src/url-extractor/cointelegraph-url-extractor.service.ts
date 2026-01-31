import { Injectable, Logger } from '@nestjs/common';
import { chromium, Browser, Page } from 'playwright';
import { IUrlExtractorService } from 'src/types';
import { AIUrlExtractorService } from './ai-url-extractor.service';

@Injectable()
export class CointelegraphUrlExtractorService implements IUrlExtractorService {
  private readonly logger = new Logger(CointelegraphUrlExtractorService.name);
  private readonly baseUrl = 'https://cointelegraph.com';
  private readonly newsListUrl =
    'https://cointelegraph.com/category/latest-news';

  constructor(private readonly aiUrlExtractor: AIUrlExtractorService) {}

  async extractUrls(): Promise<string[]> {
    let browser: Browser | null = null;
    let page: Page | null = null;
    let htmlContent = '';

    try {
      this.logger.log(
        `Starting to crawl Cointelegraph news URLs at ${this.newsListUrl}`,
      );

      browser = await chromium.launch({ headless: true });
      const context = await browser.newContext({
        userAgent:
          'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
      });
      page = await context.newPage();

      await page.goto(this.newsListUrl, {
        waitUntil: 'domcontentloaded',
        timeout: 30000,
      });

      htmlContent = await page.content();

      await page.waitForSelector('a.post-card-inline__title-link', {
        timeout: 10000,
      });

      const urls = await page.evaluate((baseUrl: string) => {
        const links = document.querySelectorAll(
          'a.post-card-inline__title-link',
        );
        const extractedUrls: string[] = [];

        links.forEach((link) => {
          const href = link.getAttribute('href');
          if (href) {
            const fullUrl = href.startsWith('/') ? `${baseUrl}${href}` : href;
            if (!extractedUrls.includes(fullUrl)) {
              extractedUrls.push(fullUrl);
            }
          }
        });

        return extractedUrls;
      }, this.baseUrl);

      this.logger.log(
        `Finished crawling Cointelegraph news URLs, extracted ${urls.length} URLs.`,
      );
      return urls;
    } catch (error) {
      this.logger.error(
        `Failed to crawl Cointelegraph using DOM-based extraction: ${error.message}`,
      );

      if (htmlContent) {
        this.logger.log('Starting fallback extraction using Gemini AI...');

        try {
          const fallbackUrls = await this.aiUrlExtractor.extractUrlsFromHtml(
            htmlContent,
            'Cointelegraph',
          );

          if (fallbackUrls.length > 0) {
            this.logger.log(
              `Finished fallback extraction using Gemini AI, found ${fallbackUrls.length} URLs.`,
            );
            return fallbackUrls;
          }
        } catch (fallbackError) {
          this.logger.error(
            `Fallback Gemini extraction also failed: ${fallbackError.message}`,
          );
        }
      }

      return [];
    } finally {
      if (browser) {
        await browser.close();
      }
    }
  }
}
