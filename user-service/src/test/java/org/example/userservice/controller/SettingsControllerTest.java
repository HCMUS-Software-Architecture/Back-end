package org.example.userservice.controller;

import org.example.userservice.dto.UpdateUserSettingsDto;
import org.example.userservice.dto.UserSettingsDto;
import org.example.userservice.service.UserSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingsControllerTest {

    @Mock
    private UserSettingsService userSettingsService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SettingsController settingsController;

    private UserSettingsDto sampleSettings;

    @BeforeEach
    void setUp() {
        sampleSettings = UserSettingsDto.builder()
                .id("settings123")
                .userId("user123")
                .defaultTimeframe("1h")
                .defaultSymbol("BTCUSDT")
                .chartTheme("dark")
                .favoriteSymbols(List.of("BTCUSDT", "ETHUSDT"))
                .enabledIndicators(List.of("MA", "RSI"))
                .showVolume(true)
                .showGrid(true)
                .emailNotifications(true)
                .language("en")
                .build();

        when(authentication.getName()).thenReturn("user123");
    }

    @Test
    void getSettings_ReturnsSettings() {
        // Given
        when(userSettingsService.getSettings("user123")).thenReturn(sampleSettings);

        // When
        ResponseEntity<UserSettingsDto> response = settingsController.getSettings(authentication);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUserId()).isEqualTo("user123");
        assertThat(response.getBody().getDefaultTimeframe()).isEqualTo("1h");
        verify(userSettingsService).getSettings("user123");
    }

    @Test
    void updateSettings_WithValidData_ReturnsUpdatedSettings() {
        // Given
        UpdateUserSettingsDto updateDto = UpdateUserSettingsDto.builder()
                .defaultTimeframe("4h")
                .chartTheme("light")
                .build();

        sampleSettings.setDefaultTimeframe("4h");
        sampleSettings.setChartTheme("light");

        when(userSettingsService.updateSettings(anyString(), any(UpdateUserSettingsDto.class)))
                .thenReturn(sampleSettings);

        // When
        ResponseEntity<UserSettingsDto> response = settingsController.updateSettings(authentication, updateDto);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDefaultTimeframe()).isEqualTo("4h");
        assertThat(response.getBody().getChartTheme()).isEqualTo("light");
        verify(userSettingsService).updateSettings(anyString(), any(UpdateUserSettingsDto.class));
    }

    @Test
    void resetSettings_ReturnsDefaultSettings() {
        // Given
        when(userSettingsService.resetToDefaults("user123")).thenReturn(sampleSettings);

        // When
        ResponseEntity<UserSettingsDto> response = settingsController.resetSettings(authentication);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUserId()).isEqualTo("user123");
        verify(userSettingsService).resetToDefaults("user123");
    }

    @Test
    void regenerateApiKey_ReturnsNewKey() {
        // Given
        sampleSettings.setApiKey("tp_newkey123");
        when(userSettingsService.regenerateApiKey("user123")).thenReturn(sampleSettings);

        // When
        ResponseEntity<UserSettingsDto> response = settingsController.regenerateApiKey(authentication);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getApiKey()).isEqualTo("tp_newkey123");
        verify(userSettingsService).regenerateApiKey("user123");
    }
}
