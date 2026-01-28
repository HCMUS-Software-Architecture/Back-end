export interface IUrlExtractorService {
  extractUrls(): Promise<string[]>;
}

export interface NewsArticle {
  header: string;
  subheader: string;
  thumbnail: string;
  content: string;
  url: string;
}

export interface INewsExtractorService {
  extractNews(url: string): Promise<NewsArticle>;
}
