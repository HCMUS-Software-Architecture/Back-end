// package com.example.backend.service;

// import com.example.backend.dto.UserDto;
// import com.example.backend.model.User;
// import com.example.backend.repository.mongodb.UserMongoRepository;
// import lombok.RequiredArgsConstructor;
// import org.apache.coyote.BadRequestException;
// import org.springframework.security.core.context.SecurityContextHolder;
// import
// org.springframework.security.core.userdetails.UsernameNotFoundException;
// import org.springframework.stereotype.Service;

// import java.util.*;

// /**
// * @deprecated SAFE TO DELETE - Migrated to user-service
// * @see
// user-service/src/main/java/org/example/userservice/service/UserService.java
// */
// @Deprecated(forRemoval = true)
// @Service
// @RequiredArgsConstructor
// public class UserService {
// private final UserMongoRepository userRepository;

// private String getUserId() {
// return (String)
// SecurityContextHolder.getContext().getAuthentication().getPrincipal();
// }

// public UserDto getUserById() throws UsernameNotFoundException {
// User user = userRepository.findUserById(getUserId())
// .orElseThrow(() -> new UsernameNotFoundException("User not found"));
// UserDto userDto = new UserDto();
// userDto.setId(user.getId());
// userDto.setEmail(user.getEmail());
// userDto.setFullName(user.getFullName());
// return userDto;
// }

// }
