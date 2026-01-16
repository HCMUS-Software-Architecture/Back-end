package com.example.backend.entity;

/**
 * DEPRECATED: PostgreSQL-based Article entity
 * 
 * This entity has been replaced by MongoDB Article model for better flexibility
 * with unstructured/semi-structured content from web crawling.
 * 
 * Location of active implementation: com.example.backend.model.Article
 * (MongoDB @Document)
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
 * import jakarta.persistence.*;
 * import lombok.AllArgsConstructor;
 * import lombok.Builder;
 * import lombok.Data;
 * import lombok.NoArgsConstructor;
 * import org.hibernate.annotations.JdbcTypeCode;
 * import org.hibernate.type.SqlTypes;
 * 
 * import java.time.LocalDateTime;
 * import java.util.Map;
 * import java.util.UUID;
 * 
 * @Entity
 * 
 * @Data
 * 
 * @NoArgsConstructor
 * 
 * @AllArgsConstructor
 * 
 * @Builder
 * public class Article {
 * 
 * @Id
 * 
 * @GeneratedValue(strategy = GenerationType.UUID)
 * private UUID id;
 * 
 * @Column(nullable = false, unique = true, length = 2048)
 * private String url;
 * 
 * @Column(length = 1024)
 * private String title;
 * 
 * @Column(columnDefinition = "TEXT")
 * private String body;
 * 
 * @Column(length = 255)
 * private String source;
 * 
 * private LocalDateTime publishedAt;
 * 
 * private LocalDateTime crawledAt;
 * 
 * @Column(columnDefinition = "TEXT")
 * private String rawHtml;
 * 
 * @JdbcTypeCode(SqlTypes.JSON)
 * 
 * @Column(columnDefinition = "jsonb")
 * private Map<String, Object> metadata;
 * 
 * private LocalDateTime createdAt;
 * 
 * private LocalDateTime updatedAt;
 * 
 * @PrePersist
 * protected void onCreate() {
 * createdAt = LocalDateTime.now();
 * updatedAt = LocalDateTime.now();
 * crawledAt = LocalDateTime.now();
 * }
 * 
 * @PreUpdate
 * protected void onUpdate() {
 * updatedAt = LocalDateTime.now();
 * }
 * }
 */
