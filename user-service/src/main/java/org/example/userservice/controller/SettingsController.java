package org.example.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.dto.UpdateUserSettingsDto;
import org.example.userservice.dto.UserSettingsDto;
import org.example.userservice.service.UserSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/settings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Settings", description = "Endpoints for managing user preferences and configuration")
@SecurityRequirement(name = "bearerAuth")
public class SettingsController {

    private final UserSettingsService userSettingsService;

    @GetMapping
    @Operation(summary = "Get user settings", description = "Retrieve current user's settings with Redis caching")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Settings retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<UserSettingsDto> getSettings(Authentication authentication) {
        String userId = authentication.getName();
        log.info("GET /api/user/settings - User: {}", userId);

        UserSettingsDto settings = userSettingsService.getSettings(userId);
        return ResponseEntity.ok(settings);
    }

    @PutMapping
    @Operation(summary = "Update user settings", description = "Update user preferences (partial update supported)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Settings updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<UserSettingsDto> updateSettings(
            Authentication authentication,
            @Valid @RequestBody UpdateUserSettingsDto updateDto) {
        String userId = authentication.getName();
        log.info("PUT /api/user/settings - User: {}", userId);

        UserSettingsDto updated = userSettingsService.updateSettings(userId, updateDto);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset settings to defaults", description = "Reset all user settings to default values")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Settings reset successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<UserSettingsDto> resetSettings(Authentication authentication) {
        String userId = authentication.getName();
        log.info("POST /api/user/settings/reset - User: {}", userId);

        UserSettingsDto reset = userSettingsService.resetToDefaults(userId);
        return ResponseEntity.ok(reset);
    }

    @PostMapping("/api-key/regenerate")
    @Operation(summary = "Regenerate API key", description = "Generate a new API key (VIP users only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "API key regenerated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "VIP subscription required")
    })
    public ResponseEntity<UserSettingsDto> regenerateApiKey(Authentication authentication) {
        String userId = authentication.getName();
        log.info("POST /api/user/settings/api-key/regenerate - User: {}", userId);

        UserSettingsDto updated = userSettingsService.regenerateApiKey(userId);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/chart")
    @Operation(summary = "Get chart preferences only", description = "Retrieve only chart-related settings")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chart preferences retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ChartPreferencesDto> getChartPreferences(Authentication authentication) {
        String userId = authentication.getName();
        log.info("GET /api/user/settings/chart - User: {}", userId);

        UserSettingsDto settings = userSettingsService.getSettings(userId);

        ChartPreferencesDto chartPrefs = ChartPreferencesDto.builder()
                .defaultTimeframe(settings.getDefaultTimeframe())
                .defaultSymbol(settings.getDefaultSymbol())
                .chartTheme(settings.getChartTheme())
                .favoriteSymbols(settings.getFavoriteSymbols())
                .enabledIndicators(settings.getEnabledIndicators())
                .showVolume(settings.getShowVolume())
                .showGrid(settings.getShowGrid())
                .build();

        return ResponseEntity.ok(chartPrefs);
    }

    // Lightweight DTO for chart preferences
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class ChartPreferencesDto {
        private String defaultTimeframe;
        private String defaultSymbol;
        private String chartTheme;
        private java.util.List<String> favoriteSymbols;
        private java.util.List<String> enabledIndicators;
        private Boolean showVolume;
        private Boolean showGrid;
    }
}
