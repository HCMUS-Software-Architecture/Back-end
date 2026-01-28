import { Injectable, Logger } from '@nestjs/common';
import { chromium, Browser, Page } from 'playwright';
import { IUrlExtractorService } from 'src/types';

@Injectable()
export class CointelegraphUrlExtractorService implements IUrlExtractorService {
  private readonly logger = new Logger(CointelegraphUrlExtractorService.name);
  private readonly baseUrl = 'https://cointelegraph.com';
  private readonly newsListUrl =
    'https://cointelegraph.com/category/latest-news';

  async extractUrls(): Promise<string[]> {
    let browser: Browser | null = null;

    try {
      this.logger.log(`Crawling ${this.newsListUrl}...`);

      browser = await chromium.launch({ headless: true });
      const context = await browser.newContext({
        userAgent:
          'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
      });
      const page: Page = await context.newPage();

      await page.goto(this.newsListUrl, {
        waitUntil: 'domcontentloaded',
        timeout: 30000,
      });

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

      this.logger.log(`Extracted ${urls.length} URLs from Cointelegraph`);
      return urls;
    } catch (error) {
      this.logger.error(`Failed to crawl Cointelegraph: ${error.message}`);
      throw error;
    } finally {
      if (browser) {
        await browser.close();
      }
    }
  }
}
