package org.example.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsDto {
    
    private String id;
    private String userId;
    
    // Chart Preferences
    private String defaultTimeframe;
    private String defaultSymbol;
    private String chartTheme;
    private List<String> favoriteSymbols;
    private List<String> enabledIndicators;
    private Integer maxIndicators;
    private Boolean showVolume;
    private Boolean showGrid;
    
    // Notification Preferences
    private Boolean emailNotifications;
    private Boolean pushNotifications;
    private Boolean priceAlerts;
    private List<PriceAlertDto> priceAlertList;
    private Boolean newsNotifications;
    private List<String> newsTopics;
    
    // Display Preferences
    private String language;
    private String timezone;
    private String dateFormat;
    private String currencyDisplay;
    private Boolean use24HourFormat;
    
    // Privacy Settings
    private Boolean profileVisible;
    private Boolean shareAnalytics;
    private Boolean showOnlineStatus;
    private Boolean allowMarketingEmails;
    
    // API Settings (VIP Only)
    private String apiKey;
    private List<String> webhookUrls;
    private Integer rateLimitTier;
    private Boolean apiAccessEnabled;
    
    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceAlertDto {
        private String symbol;
        private String condition;
        private Double targetPrice;
        private Boolean enabled;
    }
}
