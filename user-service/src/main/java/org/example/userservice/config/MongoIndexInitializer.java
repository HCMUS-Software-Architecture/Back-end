package org.example.userservice.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MongoIndexInitializer {

    private final MongoTemplate mongoTemplate;

    @PostConstruct
    public void initIndexes() {
        log.info("Initializing MongoDB indexes...");

        createUserSettingsIndexes();

        log.info("MongoDB indexes initialized successfully");
    }

    private void createUserSettingsIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps("user_settings");

        // Unique index on userId for fast lookups
        Index userIdIndex = new Index()
                .on("userId", Sort.Direction.ASC)
                .named("idx_userId")
                .unique();

        indexOps.ensureIndex(userIdIndex);

        log.info("Created index: idx_userId on user_settings.userId (unique)");

        // Compound index for favorite symbols queries (if needed for analytics)
        Index favoritesIndex = new Index()
                .on("userId", Sort.Direction.ASC)
                .on("favoriteSymbols", Sort.Direction.ASC)
                .named("idx_userId_favoriteSymbols");

        indexOps.ensureIndex(favoritesIndex);

        log.info("Created index: idx_userId_favoriteSymbols on user_settings");
    }
}
