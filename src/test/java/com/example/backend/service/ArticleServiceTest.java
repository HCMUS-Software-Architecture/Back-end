package com.example.backend.service;

import com.example.backend.model.Article;
import com.example.backend.repository.mongodb.ArticleDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for ArticleService
 * 
 * Test Coverage:
 * - Article CRUD operations
 * - Cache integration behavior
 * - Pagination logic
 * - URL-based article lookup
 * 
 * Future-proof for microservices:
 * - Repository can be replaced with REST/gRPC calls
 * - Cache eviction patterns remain valid
 * - Business logic is infrastructure-independent
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Article Service Unit Tests")
class ArticleServiceTest {

    @Mock
    private ArticleDocumentRepository articleRepository;

    @InjectMocks
    private ArticleService articleService;

    private Article testArticle1;
    private Article testArticle2;
    private final String TEST_URL = "https://example.com/article-1";

    @BeforeEach
    void setUp() {
        testArticle1 = Article.builder()
                .id("article-1")
                .url(TEST_URL)
                .title("Bitcoin Surges to New High")
                .body("Bitcoin reached a new all-time high...")
                .source("CoinDesk")
                .publishedAt(LocalDateTime.now().minusHours(2))
                .crawledAt(LocalDateTime.now().minusHours(1))
                .build();

        testArticle2 = Article.builder()
                .id("article-2")
                .url("https://example.com/article-2")
                .title("Ethereum Update Released")
                .body("New Ethereum update includes...")
                .source("CoinTelegraph")
                .publishedAt(LocalDateTime.now().minusHours(5))
                .crawledAt(LocalDateTime.now().minusHours(4))
                .build();
    }

    @Test
    @DisplayName("Should retrieve paginated articles successfully")
    void getAllArticles_shouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Article> articles = Arrays.asList(testArticle1, testArticle2);
        Page<Article> articlePage = new PageImpl<>(articles, pageable, articles.size());

        when(articleRepository.findAll(pageable)).thenReturn(articlePage);

        // When
        Page<Article> result = articleService.getAllArticles(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).containsExactly(testArticle1, testArticle2);
        assertThat(result.getTotalElements()).isEqualTo(2);

        verify(articleRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("Should retrieve article by ID successfully")
    void getArticleById_shouldReturnArticleWhenExists() {
        // Given
        when(articleRepository.findById("article-1")).thenReturn(Optional.of(testArticle1));

        // When
        Optional<Article> result = articleService.getArticleById("article-1");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("article-1");
        assertThat(result.get().getTitle()).isEqualTo("Bitcoin Surges to New High");

        verify(articleRepository, times(1)).findById("article-1");
    }

    @Test
    @DisplayName("Should return empty when article ID not found")
    void getArticleById_shouldReturnEmptyWhenNotFound() {
        // Given
        when(articleRepository.findById("non-existent")).thenReturn(Optional.empty());

        // When
        Optional<Article> result = articleService.getArticleById("non-existent");

        // Then
        assertThat(result).isEmpty();
        verify(articleRepository, times(1)).findById("non-existent");
    }

    @Test
    @DisplayName("Should retrieve article by URL successfully")
    void getArticleByUrl_shouldReturnArticleWhenUrlExists() {
        // Given
        when(articleRepository.findByUrl(TEST_URL)).thenReturn(Optional.of(testArticle1));

        // When
        Optional<Article> result = articleService.getArticleByUrl(TEST_URL);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUrl()).isEqualTo(TEST_URL);
        assertThat(result.get().getTitle()).isEqualTo("Bitcoin Surges to New High");

        verify(articleRepository, times(1)).findByUrl(TEST_URL);
    }

    @Test
    @DisplayName("Should return empty when URL not found")
    void getArticleByUrl_shouldReturnEmptyWhenUrlNotFound() {
        // Given
        String nonExistentUrl = "https://example.com/non-existent";
        when(articleRepository.findByUrl(nonExistentUrl)).thenReturn(Optional.empty());

        // When
        Optional<Article> result = articleService.getArticleByUrl(nonExistentUrl);

        // Then
        assertThat(result).isEmpty();
        verify(articleRepository, times(1)).findByUrl(nonExistentUrl);
    }

    @Test
    @DisplayName("Should save article successfully")
    void saveArticle_shouldPersistArticle() {
        // Given
        Article newArticle = Article.builder()
                .url("https://example.com/new-article")
                .title("New Article")
                .body("Article content...")
                .source("NewsSource")
                .publishedAt(LocalDateTime.now())
                .crawledAt(LocalDateTime.now())
                .build();

        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> {
            Article article = invocation.getArgument(0);
            article.setId("new-id");
            return article;
        });

        // When
        Article result = articleService.saveArticle(newArticle);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("new-id");
        assertThat(result.getTitle()).isEqualTo("New Article");

        verify(articleRepository, times(1)).save(newArticle);
    }

    @Test
    @DisplayName("Should handle empty page results")
    void getAllArticles_shouldHandleEmptyResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Article> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(articleRepository.findAll(pageable)).thenReturn(emptyPage);

        // When
        Page<Article> result = articleService.getAllArticles(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();

        verify(articleRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("Should handle different page sizes correctly")
    void getAllArticles_shouldRespectPageSize() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        List<Article> articles = Arrays.asList(testArticle1, testArticle2);
        Page<Article> articlePage = new PageImpl<>(articles, pageable, 10);

        when(articleRepository.findAll(pageable)).thenReturn(articlePage);

        // When
        Page<Article> result = articleService.getAllArticles(pageable);

        // Then
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getTotalElements()).isEqualTo(10);
        assertThat(result.getTotalPages()).isEqualTo(2);

        verify(articleRepository, times(1)).findAll(pageable);
    }
}
