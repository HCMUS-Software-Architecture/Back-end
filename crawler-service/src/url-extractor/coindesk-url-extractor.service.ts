import { Injectable, Logger } from '@nestjs/common';
import { chromium, Browser, Page } from 'playwright';
import { IUrlExtractorService } from 'src/types';

@Injectable()
export class CoindeskUrlExtractorService implements IUrlExtractorService {
  private readonly logger = new Logger(CoindeskUrlExtractorService.name);
  private readonly baseUrl = 'https://www.coindesk.com';
  private readonly newsListUrl = 'https://www.coindesk.com/latest-crypto-news';

  async extractUrls(): Promise<string[]> {
    let browser: Browser | null = null;

    try {
      this.logger.log(
        `Starting to crawl Coindesk news URLs at ${this.newsListUrl}`,
      );

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

      await page.waitForSelector('a.content-card-title', {
        timeout: 10000,
      });

      const urls = await page.evaluate((baseUrl: string) => {
        const links = document.querySelectorAll('a.content-card-title');
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
        `Finish crawling CoinDesk news URLs, found ${urls.length} URLs.`,
      );
      return urls;
    } catch (error) {
      this.logger.error(`Failed to crawl Coindesk: ${error.message}`);
      throw error;
    } finally {
      if (browser) {
        await browser.close();
      }
    }
  }
}
