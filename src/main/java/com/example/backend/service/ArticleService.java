package com.example.backend.service;

import com.example.backend.entity.Article;
import com.example.backend.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArticleService {
    private final ArticleRepository articleRepository;
    private final Logger log =  LoggerFactory.getLogger(this.getClass());

    public Page<Article> getAllArticles(Pageable pageable) {
        return articleRepository.findAll(pageable);
    }

    public Optional<Article> getArticleById(UUID id) {
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

    public Page<Article> getArticlesBySource(String source, Pageable pageable) {
        return articleRepository.findBySource(source, pageable);
    }

    public Page<Article> searchArticles(String keyword, Pageable pageable) {
        return articleRepository.searchByKeyword(keyword, pageable);
    }
}
