import { Injectable, Logger } from '@nestjs/common';
import { GoogleGenAI } from '@google/genai';
import { NewsArticle } from 'src/database/news-article.schema';

@Injectable()
export class AINewsExtractorService {
  private readonly logger = new Logger(AINewsExtractorService.name);
  private readonly modelName = process.env.GEMINI_MODEL!;
  private readonly apiKey = process.env.GEMINI_API_KEY!;
  private genAI: GoogleGenAI | null = null;

  private getClient(): GoogleGenAI {
    if (!this.genAI) {
      this.genAI = new GoogleGenAI({ apiKey: this.apiKey });
    }
    return this.genAI;
  }

  async extractNewsFromHtml(
    htmlContent: string,
    url: string,
    sourceName: string,
  ): Promise<NewsArticle> {
    try {
      this.logger.log(
        `Starting to extract news from ${sourceName} using Gemini AI...`,
      );

      const responseText = await this.getNewsFromGemini(htmlContent);

      if (!responseText) {
        this.logger.error('Empty response from Gemini API');
        throw new Error('Empty response from Gemini API');
      }

      this.logger.debug(`Gemini response: ${responseText}`);

      const articleData = this.parseNewsFromResponse(responseText);

      if (!articleData) {
        throw new Error('Failed to parse news article from Gemini response');
      }

      const article: NewsArticle = {
        header: articleData.header || '',
        subheader: articleData.subheader || '',
        thumbnail: articleData.thumbnail || '',
        content: articleData.content || '',
        url: url,
      };

      this.logger.log(
        `Extracted article "${article.header}" from ${sourceName} using Gemini`,
      );
      return article;
    } catch (error) {
      this.logger.error(
        `Failed to extract news from ${sourceName} using Gemini: ${error.message}`,
      );
      throw error;
    }
  }

  private async getNewsFromGemini(htmlContent: string): Promise<string | null> {
    const client = this.getClient();

    this.logger.log(
      'Starting to send request to Gemini API for news extraction...',
    );

    const systemPrompt = `You are a news article extraction assistant. Your task is to extract article information from HTML content.
Rules:
- Return ONLY a JSON object with the following fields: header, subheader, thumbnail, content
- header: The main title/headline of the article
- subheader: The subtitle or description of the article
- thumbnail: The URL of the main article image
- content: The full article text content (paragraphs joined with double newlines)
- Do not include any explanations or markdown formatting
- If a field cannot be found, use an empty string

Output format: {"header": "...", "subheader": "...", "thumbnail": "...", "content": "..."}`;

    const userContent = `Extract the news article information from the following HTML content:

${htmlContent}`;

    const response = await client.models.generateContent({
      model: this.modelName,
      contents: userContent,
      config: {
        systemInstruction: systemPrompt,
      },
    });

    this.logger.log(
      'Finish receiving response from Gemini API for news extraction.',
    );
    return response.text ?? null;
  }

  private parseNewsFromResponse(
    responseText: string,
  ): Partial<NewsArticle> | null {
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

      const articleData = JSON.parse(jsonString);

      if (typeof articleData !== 'object' || articleData === null) {
        throw new Error('Parsed JSON is not an object');
      }

      return articleData;
    } catch (error) {
      this.logger.error(`Failed to parse JSON from response: ${error.message}`);
      return null;
    }
  }
}
