package org.example.userservice.repository;

import org.example.userservice.model.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {
    RefreshToken findByToken(String token);
    Optional<RefreshToken> findByTokenAndIsRevokedFalse(String token);
    void removeByIsRevoked(Boolean isRevoked);
}
