package com.example.backend.repository;

/**
 * DEPRECATED: PostgreSQL-based Article repository
 * 
 * This repository has been replaced by MongoDB ArticleDocumentRepository
 * for better flexibility with unstructured/semi-structured content from web
 * crawling.
 * 
 * Location of active implementation:
 * com.example.backend.repository.mongodb.ArticleDocumentRepository
 * (MongoDB @MongoRepository)
 * 
 * Reason for deprecation:
 * - Articles are crawled dynamically from various internet sources and forums
 * - Content structure varies significantly between sources
 * - MongoDB's schema flexibility better suits unstructured data
 * - Faster writes for high-frequency crawling operations
 * 
 * Migration status: COMMENTED OUT (kept for reference during Phase 2)
 * Removal planned: Phase 3 (after full migration verification)
 * 
 * Last active: Phase 1 (before MongoDB integration)
 */

/*
 * import com.example.backend.entity.Article;
 * import org.springframework.data.domain.Page;
 * import org.springframework.data.domain.Pageable;
 * import org.springframework.data.jpa.repository.JpaRepository;
 * import org.springframework.data.jpa.repository.Query;
 * import org.springframework.stereotype.Repository;
 * 
 * import java.time.LocalDateTime;
 * import java.util.Optional;
 * import java.util.UUID;
 * 
 * @Repository
 * public interface ArticleRepository extends JpaRepository<Article, UUID> {
 * Optional<Article> findByUrl(String url);
 * 
 * boolean existsByUrl(String url);
 * 
 * Page<Article> findBySource(String source, Pageable pageable);
 * 
 * Page<Article> findByPublishedAtBetween(
 * LocalDateTime start,
 * LocalDateTime end,
 * Pageable pageable
 * );
 * 
 * @Query("SELECT a FROM Article a WHERE a.title LIKE CONCAT('%', :keyword, '%') OR a.body LIKE CONCAT('%', :keyword, '%')"
 * )
 * Page<Article> searchByKeyword(String keyword, Pageable pageable);
 * }
 */
