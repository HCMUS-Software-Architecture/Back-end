package org.example.userservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserSettingsDto {

    // Chart Preferences
    @Pattern(regexp = "1m|5m|15m|30m|1h|4h|1d|1w", message = "Invalid timeframe")
    private String defaultTimeframe;

    @Size(max = 20, message = "Symbol name too long")
    private String defaultSymbol;

    @Pattern(regexp = "dark|light", message = "Theme must be 'dark' or 'light'")
    private String chartTheme;

    @Size(max = 50, message = "Maximum 50 favorite symbols")
    private List<String> favoriteSymbols;

    @Size(max = 20, message = "Too many indicators")
    private List<String> enabledIndicators;

    private Boolean showVolume;
    private Boolean showGrid;

    // Notification Preferences
    private Boolean emailNotifications;
    private Boolean pushNotifications;
    private Boolean priceAlerts;
    private List<UserSettingsDto.PriceAlertDto> priceAlertList;
    private Boolean newsNotifications;

    @Size(max = 10, message = "Maximum 10 news topics")
    private List<String> newsTopics;

    // Display Preferences
    @Pattern(regexp = "en|es|fr|de|zh|ja|ko|pt|ru", message = "Unsupported language")
    private String language;

    private String timezone;
    private String dateFormat;

    @Pattern(regexp = "USD|EUR|GBP|JPY|CNY|KRW|BTC|ETH", message = "Unsupported currency")
    private String currencyDisplay;

    private Boolean use24HourFormat;

    // Privacy Settings
    private Boolean profileVisible;
    private Boolean shareAnalytics;
    private Boolean showOnlineStatus;
    private Boolean allowMarketingEmails;

    // API Settings (VIP Only) - these are handled separately
    @Size(max = 5, message = "Maximum 5 webhook URLs")
    private List<String> webhookUrls;
}
