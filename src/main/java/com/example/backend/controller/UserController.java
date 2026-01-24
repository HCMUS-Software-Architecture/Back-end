// package com.example.backend.controller;

// import com.example.backend.dto.UserDto;
// import com.example.backend.service.UserService;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.apache.coyote.BadRequestException;
// import org.springframework.http.ResponseEntity;
// import
// org.springframework.security.core.userdetails.UsernameNotFoundException;
// import org.springframework.web.bind.annotation.*;

// import java.util.Map;

// /**
// * @deprecated SAFE TO DELETE - Migrated to user-service
// * @see
// user-service/src/main/java/org/example/userservice/controller/UserController.java
// */
// @Deprecated(forRemoval = true)
// @RestController
// @RequiredArgsConstructor
// @Slf4j
// @RequestMapping("/api/me")
// public class UserController {
// private final UserService userService;

// @GetMapping
// public ResponseEntity<UserDto> getCurrentUser() {
// return ResponseEntity.ok().body(userService.getUserById());
// }

// // @PostMapping
// // public ResponseEntity<?> updateUserInfo(@RequestBody Map<String, String>
// // body) throws BadRequestException {
// // String newName = body.get("fullName");
// // userService.updateFullName(newName);
// // return ResponseEntity.ok().build();
// // }

// }
