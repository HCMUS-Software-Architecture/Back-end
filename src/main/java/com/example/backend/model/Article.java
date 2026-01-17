package com.example.backend.model;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "articles")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Article {
    @Id
    private String id;

    @Indexed(unique = true)
    private String url;

    private String title;
    private String body;

    @Indexed
    private String source;

    private String rawHtml;

    @Indexed
    private LocalDateTime publishedAt;

    private LocalDateTime crawledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Flexible metadata
    private Map<String, Object> metadata;

    // NLP results (Phase 3+)
    private SentimentResult sentiment;
    private List<String> entities;
    private List<String> symbols;
    private List<String> tags;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SentimentResult {
        private String label; // bullish, bearish, neutral
        private double score;
        private String model;
    }
    
    // Convenience methods for NLP analysis
    
    /**
     * Get content for NLP analysis (title + body)
     */
    public String getContent() {
        return body;
    }
    
    /**
     * Get summary (first 200 chars of body)
     */
    public String getSummary() {
        if (body == null || body.isEmpty()) {
            return null;
        }
        return body.length() > 200 ? body.substring(0, 200) + "..." : body;
    }
}
