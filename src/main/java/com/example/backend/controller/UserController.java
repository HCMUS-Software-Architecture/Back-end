package com.example.backend.controller;

import com.example.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/me")
public class UserController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getCurrentUser() {
        return ResponseEntity.ok().body(userService.getUserById());
    }

//    @PostMapping
//    public ResponseEntity<?> updateUserInfo(@RequestBody Map<String, String> body) throws BadRequestException {
//        String newName = body.get("fullName");
//        userService.updateFullName(newName);
//        return ResponseEntity.ok().build();
//    }

}
