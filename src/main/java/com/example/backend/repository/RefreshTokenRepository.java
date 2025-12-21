package com.example.backend.repository;

import com.example.backend.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    RefreshToken findByToken(String token);

    @Query("select r from RefreshToken r where r.is_revoke = false and r.userId = :userId")
    Optional<RefreshToken> findByUserId(String userId);
}
