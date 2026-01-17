package org.example.userservice.repository;

import org.example.userservice.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findUserById(String id);
    Optional<User> findByEmail(String email);
}
