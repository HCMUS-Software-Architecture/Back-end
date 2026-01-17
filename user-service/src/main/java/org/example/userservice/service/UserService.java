package org.example.userservice.service;

import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.UserDto;
import org.example.userservice.model.User;
import org.example.userservice.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    private String getUserId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
    public UserDto getUserById() throws UsernameNotFoundException {
        User user = userRepository.findUserById(getUserId()).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setEmail(user.getEmail());
        userDto.setFullName(user.getFullName());
        return userDto;
    }
}
