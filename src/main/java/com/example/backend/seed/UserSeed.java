package com.example.backend.seed;

import com.example.backend.entity.AuthUser;
import com.example.backend.entity.User;
import com.example.backend.repository.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;


@Component
@RequiredArgsConstructor
@Slf4j
public class UserSeed implements CommandLineRunner {
    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("Seed begin");

        Optional<AuthUser> authUserOptional = authUserRepository.findByEmail("example@gmail.com");
        if(authUserOptional.isPresent()){
            log.info("User already exists, seed end");
            return;
        }

        AuthUser authUser = new AuthUser();
        User user = new User();
        user.setFullName("Nguyen Van A");

        authUser.setUser(user);
        authUser.setEmail("example@gmail.com");
        authUser.setPassword(passwordEncoder.encode("123456"));
        authUserRepository.save(authUser);

        log.info("Seed end");
    }
}
