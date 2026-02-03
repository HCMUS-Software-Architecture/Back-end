package org.example.userservice.service;

import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.UpdateProfileDto;
import org.example.userservice.dto.UserDto;
import org.example.userservice.model.User;
import org.example.userservice.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserSettingsService userSettingsService;

    private String getUserId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public UserDto getUserById() throws UsernameNotFoundException {
        User user = userRepository.findUserById(getUserId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return mapToUserDto(user);
    }

    /**
     * Update user profile (currently only fullName)
     */
    @Transactional
    public UserDto updateProfile(UpdateProfileDto updateDto) throws UsernameNotFoundException {
        String userId = getUserId();
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        user.setFullName(updateDto.getFullName());
        user.setUpdatedAt(java.time.LocalDateTime.now());

        User saved = userRepository.save(user);
        return mapToUserDto(saved);
    }

    /**
     * Delete user account and all associated data (settings, refresh tokens, etc.)
     */
    @Transactional
    public void deleteAccount() throws UsernameNotFoundException {
        String userId = getUserId();
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Delete user settings (also evicts from cache)
        userSettingsService.deleteSettings(userId);

        // Delete user (cascade will handle refresh_token and password_reset_tokens)
        userRepository.delete(user);
    }

    /**
     * Map User entity to UserDto including subscription info
     */
    private UserDto mapToUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .subscriptionType(user.getSubscriptionType())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .phone(user.getPhone())
                .country(user.getCountry())
                .build();
    }
}
