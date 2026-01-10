package com.example.backend.service;

import com.example.backend.model.Article;
import com.example.backend.repository.ArticleDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Article Service with Redis Caching
 * 
 * Cache Strategy (Cache-Aside Pattern):
 * - getAllArticles: Cache paginated results for 5 minutes
 * - getArticleById: Cache individual articles for 15 minutes
 * - saveArticle: Evict cache on save to ensure data consistency
 * 
 * Reference: Phase2-ImplementationGuide.md - Cache-aside Pattern
 */
@Service
@RequiredArgsConstructor
public class ArticleService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final ArticleDocumentRepository articleRepository;

    /**
     * Get all articles with pagination
     * 
     * Cache: "articles" (5-minute TTL)
     * Key: page number and size (e.g., "articles::0_10")
     * 
     * Why cache?
     * - Reduces MongoDB load for frequently accessed pages
     * - Improves response time for list views
     */
    @Cacheable(value = "articles", key = "#pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<Article> getAllArticles(Pageable pageable) {
        log.debug("Fetching articles from database - Page: {}, Size: {}",
                pageable.getPageNumber(), pageable.getPageSize());
        return articleRepository.findAll(pageable);
    }

    /**
     * Get article by ID
     * 
     * Cache: "article" (15-minute TTL)
     * Key: article ID
     * 
     * Why cache?
     * - Individual articles rarely change after creation
     * - High read frequency for popular articles
     */
    @Cacheable(value = "article", key = "#id")
    public Optional<Article> getArticleById(String id) {
        log.debug("Fetching article from database - ID: {}", id);
        return articleRepository.findById(id);
    }

    public Optional<Article> getArticleByUrl(String url) {
        return articleRepository.findByUrl(url);
    }

    /**
     * Save article and evict cache
     * 
     * Cache Eviction:
     * - Evicts "articles" cache (all pages) to reflect new article
     * - Does NOT evict individual "article" cache (new article won't be cached yet)
     * 
     * Why evict?
     * - Ensures consistency: new articles appear in list immediately
     * - Cache-aside pattern: write-through invalidation
     */
    @Transactional
    @CacheEvict(value = "articles", allEntries = true)
    public Article saveArticle(Article article) {
        // Check for duplicate
        if (articleRepository.existsByUrl(article.getUrl())) {
            log.info("Article already exists: {}", article.getUrl());
            return articleRepository.findByUrl(article.getUrl()).orElse(null);
        }
        log.info("Saving new article and evicting cache: {}", article.getUrl());
        return articleRepository.save(article);
    }

    public PagedModel<Article> getArticlesBySource(String source, Pageable pageable) {
        Page<Article> articles = articleRepository.findBySource(source, pageable);
        return new PagedModel<>(articles);
    }

    public PagedModel<Article> searchArticles(String keyword, Pageable pageable) {
        Page<Article> articles = articleRepository.searchByKeyword(keyword, pageable);
        return new PagedModel<>(articles);
    }

    /**
     * Delete old articles and evict cache
     * 
     * Scheduled: Every hour (0 0 * * * *)
     * Cache Eviction: Clear all articles cache after deletion
     */
    @Transactional
    @Scheduled(cron = "0 0 * * * *")
    @CacheEvict(value = "articles", allEntries = true)
    public void deleteArticlePeriodic() {
        LocalDateTime now = LocalDateTime.now().minusDays(3);
        log.info("Deleting articles older than {} and evicting cache", now);
        articleRepository.deleteByCreatedAtBefore(now);
    }
}
