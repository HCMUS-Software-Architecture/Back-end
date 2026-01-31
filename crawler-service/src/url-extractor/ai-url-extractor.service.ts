import { Injectable, Logger } from '@nestjs/common';
import { GoogleGenAI } from '@google/genai';

@Injectable()
export class AIUrlExtractorService {
  private readonly logger = new Logger(AIUrlExtractorService.name);
  private readonly modelName = process.env.GEMINI_MODEL!;
  private readonly apiKey = process.env.GEMINI_API_KEY!;

  private getClient(): GoogleGenAI {
    return new GoogleGenAI({ apiKey: this.apiKey });
  }

  async extractUrlsFromHtml(
    htmlContent: string,
    sourceName: string,
  ): Promise<string[]> {
    try {
      this.logger.log(
        `Starting to extract URLs from ${sourceName} using Gemini AI...`,
      );

      const responseText = await this.getUrlsFromGemini(htmlContent);

      if (!responseText) {
        this.logger.error('Empty response from Gemini API');
        return [];
      }

      this.logger.debug(`Gemini response: ${responseText}`);

      const urls = this.parseUrlsFromResponse(responseText);

      const validUrls = this.filterValidUrls(urls);

      this.logger.log(
        `Extracted ${validUrls.length} URLs from ${sourceName} using Gemini`,
      );
      return validUrls;
    } catch (error) {
      this.logger.error(
        `Failed to extract URLs from ${sourceName} using Gemini: ${error.message}`,
      );
      return [];
    }
  }

  private async getUrlsFromGemini(htmlContent: string): Promise<string | null> {
    const client = this.getClient();

    this.logger.log(
      'Starting to sending request to Gemini API for URL extraction...',
    );

    const systemPrompt = `You are a URL extraction assistant. Your task is to extract news article URLs from HTML content.
Rules:
- Return ONLY a JSON array of URLs, nothing else
- Each URL should be a complete, absolute URL
- If no URLs can be found, return an empty array []
- Do not include any explanations or markdown formatting

Output format: ["url1", "url2", "url3"]`;

    const userContent = `Extract all news article URLs from the following HTML content:

${htmlContent}`;

    const response = await client.models.generateContent({
      model: this.modelName,
      contents: userContent,
      config: {
        systemInstruction: systemPrompt,
      },
    });

    this.logger.log(
      'Finish receiving response from Gemini API for URL extraction.',
    );
    return response.text ?? null;
  }

  private parseUrlsFromResponse(responseText: string): string[] {
    try {
      let jsonString = responseText.trim();

      if (jsonString.startsWith('```json')) {
        jsonString = jsonString.slice(7);
      } else if (jsonString.startsWith('```')) {
        jsonString = jsonString.slice(3);
      }
      if (jsonString.endsWith('```')) {
        jsonString = jsonString.slice(0, -3);
      }
      jsonString = jsonString.trim();

      const urlArray = JSON.parse(jsonString);

      if (!Array.isArray(urlArray)) {
        throw new Error('Parsed JSON is not an array');
      }

      return urlArray;
    } catch (error) {
      this.logger.error(`Failed to parse JSON from response: ${error.message}`);
      return [];
    }
  }

  private filterValidUrls(urls: unknown[]): string[] {
    return urls.filter((url): url is string => {
      if (typeof url !== 'string') {
        return false;
      }
      try {
        new URL(url);
        return true;
      } catch {
        this.logger.warn(`Invalid URL skipped: ${url}`);
        return false;
      }
    });
  }
}
