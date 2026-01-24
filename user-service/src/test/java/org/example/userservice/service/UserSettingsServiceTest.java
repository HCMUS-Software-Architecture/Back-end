package org.example.userservice.service;

import org.example.userservice.dto.UpdateUserSettingsDto;
import org.example.userservice.dto.UserSettingsDto;
import org.example.userservice.exception.ResourceNotFoundException;
import org.example.userservice.model.SubscriptionType;
import org.example.userservice.model.User;
import org.example.userservice.model.UserSettings;
import org.example.userservice.repository.UserRepository;
import org.example.userservice.repository.UserSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSettingsServiceTest {

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserSettingsService userSettingsService;

    private User testUser;
    private UserSettings testSettings;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user123")
                .email("test@example.com")
                .fullName("Test User")
                .subscriptionType(SubscriptionType.REGULAR)
                .build();

        testSettings = UserSettings.builder()
                .id("settings123")
                .userId("user123")
                .defaultTimeframe("1h")
                .defaultSymbol("BTCUSDT")
                .chartTheme("dark")
                .build();
    }

    @Test
    void getSettings_WhenSettingsExist_ReturnsSettings() {
        // Given
        when(userSettingsRepository.findByUserId("user123")).thenReturn(Optional.of(testSettings));

        // When
        UserSettingsDto result = userSettingsService.getSettings("user123");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo("user123");
        assertThat(result.getDefaultTimeframe()).isEqualTo("1h");
        assertThat(result.getDefaultSymbol()).isEqualTo("BTCUSDT");
        verify(userSettingsRepository).findByUserId("user123");
    }

    @Test
    void getSettings_WhenSettingsNotExist_CreatesDefaultSettings() {
        // Given
        when(userSettingsRepository.findByUserId("user123")).thenReturn(Optional.empty());

        // When
        UserSettingsDto result = userSettingsService.getSettings("user123");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo("user123");
        assertThat(result.getDefaultTimeframe()).isEqualTo("1h"); // Default value
        verify(userSettingsRepository).findByUserId("user123");
    }

    @Test
    void updateSettings_WithValidData_UpdatesAndReturns() {
        // Given
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));
        when(userSettingsRepository.findByUserId("user123")).thenReturn(Optional.of(testSettings));
        when(userSettingsRepository.save(any(UserSettings.class))).thenReturn(testSettings);

        UpdateUserSettingsDto updateDto = UpdateUserSettingsDto.builder()
                .defaultTimeframe("4h")
                .chartTheme("light")
                .build();

        // When
        UserSettingsDto result = userSettingsService.updateSettings("user123", updateDto);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).findById("user123");
        verify(userSettingsRepository).save(any(UserSettings.class));
    }

    @Test
    void updateSettings_WhenUserNotFound_ThrowsException() {
        // Given
        when(userRepository.findById("user123")).thenReturn(Optional.empty());

        UpdateUserSettingsDto updateDto = UpdateUserSettingsDto.builder()
                .defaultTimeframe("4h")
                .build();

        // When & Then
        assertThatThrownBy(() -> userSettingsService.updateSettings("user123", updateDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void handleSubscriptionChange_ToVIP_GeneratesApiKey() {
        // Given
        testSettings.setApiKey(null);
        when(userSettingsRepository.findByUserId("user123")).thenReturn(Optional.of(testSettings));
        when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UserSettingsDto result = userSettingsService.handleSubscriptionChange("user123", SubscriptionType.VIP);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getApiKey()).isNotNull();
        assertThat(result.getApiKey()).startsWith("tp_");
        assertThat(result.getApiAccessEnabled()).isTrue();
        assertThat(result.getMaxIndicators()).isEqualTo(Integer.MAX_VALUE);
        verify(userSettingsRepository).save(any(UserSettings.class));
    }

    @Test
    void handleSubscriptionChange_ToREGULAR_RemovesApiKey() {
        // Given
        testSettings.setApiKey("tp_existingkey123");
        testSettings.setMaxIndicators(Integer.MAX_VALUE);
        when(userSettingsRepository.findByUserId("user123")).thenReturn(Optional.of(testSettings));
        when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UserSettingsDto result = userSettingsService.handleSubscriptionChange("user123", SubscriptionType.REGULAR);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getApiKey()).isNull();
        assertThat(result.getApiAccessEnabled()).isFalse();
        assertThat(result.getMaxIndicators()).isEqualTo(3);
        verify(userSettingsRepository).save(any(UserSettings.class));
    }

    @Test
    void resetToDefaults_DeletesOldAndCreatesNew() {
        // Given
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));
        when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UserSettingsDto result = userSettingsService.resetToDefaults("user123");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo("user123");
        verify(userSettingsRepository).deleteByUserId("user123");
        verify(userSettingsRepository).save(any(UserSettings.class));
    }

    @Test
    void regenerateApiKey_ForVIPUser_GeneratesNewKey() {
        // Given
        testUser.setSubscriptionType(SubscriptionType.VIP);
        testSettings.setApiKey("tp_oldkey123");
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));
        when(userSettingsRepository.findByUserId("user123")).thenReturn(Optional.of(testSettings));
        when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UserSettingsDto result = userSettingsService.regenerateApiKey("user123");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getApiKey()).isNotNull();
        assertThat(result.getApiKey()).startsWith("tp_");
        assertThat(result.getApiKey()).isNotEqualTo("tp_oldkey123");
        verify(userSettingsRepository).save(any(UserSettings.class));
    }

    @Test
    void regenerateApiKey_ForRegularUser_ThrowsException() {
        // Given
        testUser.setSubscriptionType(SubscriptionType.REGULAR);
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> userSettingsService.regenerateApiKey("user123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("VIP users");
    }

    @Test
    void deleteSettings_RemovesUserSettings() {
        // When
        userSettingsService.deleteSettings("user123");

        // Then
        verify(userSettingsRepository).deleteByUserId("user123");
    }

    @Test
    void applyTierRestrictions_Regular_LimitsIndicators() {
        // Given
        testSettings.setEnabledIndicators(List.of("MA", "RSI", "MACD", "BB", "STOCH")); // 5 indicators
        testSettings.setFavoriteSymbols(
                List.of("BTC", "ETH", "ADA", "SOL", "MATIC", "LINK", "DOT", "UNI", "AVAX", "ATOM", "XRP", "DOGE")); // 12
                                                                                                                    // symbols

        // When
        testSettings.applyTierRestrictions(SubscriptionType.REGULAR);

        // Then
        assertThat(testSettings.getEnabledIndicators()).hasSize(3);
        assertThat(testSettings.getFavoriteSymbols()).hasSize(10);
        assertThat(testSettings.getMaxIndicators()).isEqualTo(3);
        assertThat(testSettings.getApiAccessEnabled()).isFalse();
        assertThat(testSettings.getApiKey()).isNull();
    }

    @Test
    void applyTierRestrictions_VIP_AllowsUnlimited() {
        // Given
        testSettings.setEnabledIndicators(List.of("MA", "RSI", "MACD", "BB", "STOCH"));
        testSettings.setFavoriteSymbols(List.of("BTC", "ETH", "ADA", "SOL", "MATIC"));

        // When
        testSettings.applyTierRestrictions(SubscriptionType.VIP);

        // Then
        assertThat(testSettings.getEnabledIndicators()).hasSize(5); // Not truncated
        assertThat(testSettings.getFavoriteSymbols()).hasSize(5);
        assertThat(testSettings.getMaxIndicators()).isEqualTo(Integer.MAX_VALUE);
        assertThat(testSettings.getApiAccessEnabled()).isTrue();
    }
}
