package com.example.backend.service;

import com.example.backend.entity.Article;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CrawlerService {
    private final ArticleService articleService;
    private final Logger log = LoggerFactory.getLogger(CrawlerService.class);

    // Source configurations (can be externalized)
    private static final Map<String, SourceConfig> SOURCES = Map.of(
            "coindesk", new SourceConfig(
                    "https://www.coindesk.com/markets/",
                    "div.row-span-1, div.row-span-2",
                    "a",
                    ".article-body, .content"
            )
    );

    @Scheduled(fixedDelayString = "${crawler.fixed-delay:300000}",
            initialDelayString = "${crawler.initial-delay:60000}")
    public void scheduledCrawl() {
        log.info("Starting scheduled crawl at {}", LocalDateTime.now());
        SOURCES.forEach((name, config) -> {
            try {
                crawlSource(name, config);
            } catch (Exception e) {
                log.error("Failed to crawl source: {}", name, e);
            }
        });
        log.info("Completed scheduled crawl at {}", LocalDateTime.now());
    }

    public void crawlSource(String sourceName, SourceConfig config) {
        try {
            log.info("Crawling source: {}", sourceName);

            Document doc = Jsoup.connect(config.baseUrl())
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(30000)
                    .get();

            Elements articles = doc.select(config.articleSelector());
            log.info("Found {} articles from {}", articles.size(), sourceName);

            for (Element articleElement : articles) {
                try {
                    processArticleElement(articleElement, sourceName, config);
                } catch (Exception e) {
                    log.warn("Failed to process article element from {}: {}", sourceName, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error crawling {}: {}", sourceName, e.getMessage());
        }
    }

    private void processArticleElement(Element element, String source, SourceConfig config) {
        // Extract URL
        Element link = element.selectFirst("a[href]");
        if (link == null) return;

        String url = link.absUrl("href");
        if (url.isEmpty()) return;

        // Extract title
        Element titleElement = element.selectFirst(config.titleSelector());
        String title = titleElement != null ? titleElement.text() : "";

        // Build article
        Article article = Article.builder()
                .url(url)
                .title(title)
                .source(source)
                .rawHtml(element.outerHtml())
                .metadata(new HashMap<>(Map.of(
                        "crawledBy", "basic-crawler",
                        "extractedAt", LocalDateTime.now().toString()
                )))
                .build();

        // Save (with deduplication)
        Article saved = articleService.saveArticle(article);
        if (saved != null && saved.getId() != null) {
            log.debug("Saved article: {}", title);
        }
    }

    public record SourceConfig(
            String baseUrl,
            String articleSelector,
            String titleSelector,
            String bodySelector
    ) {}
}
