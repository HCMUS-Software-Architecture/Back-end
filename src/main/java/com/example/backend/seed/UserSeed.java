package com.example.backend.seed;

import com.example.backend.model.User;
import com.example.backend.repository.mongodb.UserMongoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Optional;


@Component
@RequiredArgsConstructor
@Slf4j
public class UserSeed implements CommandLineRunner {
    private final UserMongoRepository userMongoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("Seed begin");
        userMongoRepository.deleteAll();

        Optional<User> authUserOptional = userMongoRepository.findByEmail("example@gmail.com");
        if(authUserOptional.isPresent()){
            log.info("User already exists, seed end");
            return;
        }

        User user = new User();
        user.setFullName("Nguyen Van A");
        user.setEmail("example@gmail.com");
        user.setPassword(passwordEncoder.encode("123456"));

        userMongoRepository.save(user);

        log.info("Seed end");
    }
}
