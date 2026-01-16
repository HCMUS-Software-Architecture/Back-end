package com.example.backend.repository.mongodb;

import com.example.backend.model.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ArticleDocumentRepository extends MongoRepository<Article, String> {
    Optional<Article> findByUrl(String url);

    boolean existsByUrl(String url);

    Page<Article> findBySource(String source, Pageable pageable);

    Page<Article> findByPublishedAtBetween(
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );

    @Query("{ $or: [ { 'title': { $regex: ?0, $options: 'i' } }, { 'body': { $regex: ?0, $options: 'i' } } ] }")
    Page<Article> searchByKeyword(String keyword, Pageable pageable);

    List<Article> findBySymbolsContaining(String symbol);

    @Query("{ 'sentiment.label': ?0 }")
    Page<Article> findBySentiment(String sentiment, Pageable pageable);

    void deleteByCreatedAtBefore(LocalDateTime createdAt);
}
