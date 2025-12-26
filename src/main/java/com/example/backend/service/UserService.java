package com.example.backend.service;

import com.example.backend.dto.UserDto;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    private UUID getUserId() {
        String userIdFromContext = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return UUID.fromString(userIdFromContext);
    }
    public UserDto getUserById() throws UsernameNotFoundException {
        User user = userRepository.findById(getUserId()).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setEmail(user.getAuthUser().getEmail());
        userDto.setFullName(user.getFullName());
        return userDto;
    }

}
