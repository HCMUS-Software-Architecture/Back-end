package com.example.backend.repository.mongodb;

import com.example.backend.model.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RefreshTokenMongoRepository extends MongoRepository<RefreshToken, String> {
    RefreshToken findByToken(String token);
    Optional<RefreshToken> findByTokenAndIsRevokedFalse(String token);
    void removeByIsRevoked(Boolean isRevoked);
}
