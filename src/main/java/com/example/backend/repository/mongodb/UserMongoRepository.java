package com.example.backend.repository.mongodb;

import com.example.backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserMongoRepository extends MongoRepository<User, String> {
    Optional<User> findUserById(String id);
    Optional<User> findByEmail(String email);
}
