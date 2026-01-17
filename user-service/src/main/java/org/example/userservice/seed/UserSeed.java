package org.example.userservice.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.model.User;
import org.example.userservice.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;


@Component
@RequiredArgsConstructor
@Slf4j
public class UserSeed implements CommandLineRunner {
    private final UserRepository userMongoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("Seed begin");

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
