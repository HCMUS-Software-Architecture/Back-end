package com.example.backend.service;

import com.example.backend.model.Article;
import com.example.backend.repository.mongodb.ArticleDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ArticleService {
    private final Logger log =  LoggerFactory.getLogger(this.getClass());
    private final ArticleDocumentRepository articleRepository;

    public Page<Article> getAllArticles(Pageable pageable) {
        return articleRepository.findAll(pageable);
    }

    public Optional<Article> getArticleById(String id) {
        return articleRepository.findById(id);
    }

    public Optional<Article> getArticleByUrl(String url) {
        return articleRepository.findByUrl(url);
    }

    @Transactional
    public Article saveArticle(Article article) {
        // Check for duplicate
        if (articleRepository.existsByUrl(article.getUrl())) {
            log.info("Article already exists: {}", article.getUrl());
            return articleRepository.findByUrl(article.getUrl()).orElse(null);
        }
        return articleRepository.save(article);
    }

    public PagedModel<Article> getArticlesBySource(String source, Pageable pageable) {
        Page<Article> articles = articleRepository.findBySource(source, pageable);
        return new PagedModel<>(articles);
    }

    public PagedModel<Article> searchArticles(String keyword, Pageable pageable) {
        Page<Article> articles = articleRepository.searchByKeyword(keyword, pageable);
        return new  PagedModel<>(articles);
    }

    @Transactional
    @Scheduled(cron = "0 0 * * * *")
    public void deleteArticlePeriodic() {
        LocalDateTime now = LocalDateTime.now().minusDays(3);
        articleRepository.deleteByCreatedAtBefore(now);
    }
}
