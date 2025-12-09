package com.example.backend.repository;

import com.example.backend.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArticleRepository extends JpaRepository<Article, UUID> {
    Optional<Article> findByUrl(String url);

    boolean existsByUrl(String url);

    Page<Article> findBySource(String source, Pageable pageable);

    Page<Article> findByPublishedAtBetween(
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );

    @Query("SELECT a FROM Article a WHERE a.title LIKE CONCAT('%', :keyword, '%') OR a.body LIKE CONCAT('%', :keyword, '%')")
    Page<Article> searchByKeyword(String keyword, Pageable pageable);
}
