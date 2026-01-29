package org.example.userservice.repository;

import org.example.userservice.model.PasswordResetToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {

    Optional<PasswordResetToken> findByTokenAndUsedFalse(String token);

    Optional<PasswordResetToken> findByToken(String token);

    void deleteByUserId(String userId);

    void deleteByEmail(String email);
}
