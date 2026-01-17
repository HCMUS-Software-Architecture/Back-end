package com.example.backend.service;

import com.example.backend.dto.UserDto;
import com.example.backend.model.User;
import com.example.backend.repository.mongodb.UserMongoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for UserService
 * 
 * Test Coverage:
 * - User retrieval by authenticated user ID
 * - Security context integration
 * - Error handling for non-existent users
 * 
 * Future-proof for microservices:
 * - Tests authorization flow (portable to API Gateway)
 * - User lookup can be replaced with User Service calls
 * - DTO mapping remains consistent
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("User Service Unit Tests")
class UserServiceTest {

    @Mock
    private UserMongoRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private final String TEST_USER_ID = "user-123";
    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_FULL_NAME = "Test User";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(TEST_USER_ID)
                .email(TEST_EMAIL)
                .fullName(TEST_FULL_NAME)
                .password("encoded-password")
                .build();

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("Should retrieve user by authenticated user ID successfully")
    void getUserById_shouldReturnUserDtoForAuthenticatedUser() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(TEST_USER_ID);
        when(userRepository.findUserById(TEST_USER_ID)).thenReturn(Optional.of(testUser));

        // When
        UserDto result = userService.getUserById();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_USER_ID);
        assertThat(result.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(result.getFullName()).isEqualTo(TEST_FULL_NAME);

        verify(userRepository, times(1)).findUserById(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void getUserById_shouldThrowExceptionWhenUserNotFound() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(TEST_USER_ID);
        when(userRepository.findUserById(TEST_USER_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserById())
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository, times(1)).findUserById(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should extract user ID from security context correctly")
    void getUserById_shouldExtractUserIdFromSecurityContext() {
        // Given
        String differentUserId = "user-456";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(differentUserId);

        User differentUser = User.builder()
                .id(differentUserId)
                .email("different@example.com")
                .fullName("Different User")
                .build();

        when(userRepository.findUserById(differentUserId)).thenReturn(Optional.of(differentUser));

        // When
        UserDto result = userService.getUserById();

        // Then
        assertThat(result.getId()).isEqualTo(differentUserId);
        verify(userRepository, times(1)).findUserById(differentUserId);
        verify(userRepository, never()).findUserById(TEST_USER_ID);
    }
}
