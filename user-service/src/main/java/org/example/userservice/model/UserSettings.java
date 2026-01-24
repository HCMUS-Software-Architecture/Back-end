package org.example.userservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "user_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettings {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String userId;
    
    // === Chart Preferences ===
    @Builder.Default
    private String defaultTimeframe = "1h";  // 1m, 5m, 15m, 1h, 4h, 1d, 1w
    
    @Builder.Default
    private String defaultSymbol = "BTCUSDT";
    
    @Builder.Default
    private String chartTheme = "dark";  // dark, light
    
    @Builder.Default
    private List<String> favoriteSymbols = new ArrayList<>();
    
    @Builder.Default
    private List<String> enabledIndicators = new ArrayList<>();  // MA, RSI, MACD, BB, etc.
    
    @Builder.Default
    private Integer maxIndicators = 3;  // REGULAR: 3, VIP: unlimited
    
    @Builder.Default
    private Boolean showVolume = true;
    
    @Builder.Default
    private Boolean showGrid = true;
    
    // === Notification Preferences ===
    @Builder.Default
    private Boolean emailNotifications = true;
    
    @Builder.Default
    private Boolean pushNotifications = false;
    
    @Builder.Default
    private Boolean priceAlerts = false;
    
    @Builder.Default
    private List<PriceAlert> priceAlertList = new ArrayList<>();
    
    @Builder.Default
    private Boolean newsNotifications = true;
    
    @Builder.Default
    private List<String> newsTopics = new ArrayList<>();  // crypto, stocks, forex, etc.
    
    // === Display Preferences ===
    @Builder.Default
    private String language = "en";  // en, es, fr, de, etc.
    
    @Builder.Default
    private String timezone = "UTC";
    
    @Builder.Default
    private String dateFormat = "yyyy-MM-dd HH:mm:ss";
    
    @Builder.Default
    private String currencyDisplay = "USD";
    
    @Builder.Default
    private Boolean use24HourFormat = true;
    
    // === Privacy Settings ===
    @Builder.Default
    private Boolean profileVisible = false;
    
    @Builder.Default
    private Boolean shareAnalytics = false;
    
    @Builder.Default
    private Boolean showOnlineStatus = false;
    
    @Builder.Default
    private Boolean allowMarketingEmails = false;
    
    // === API Settings (VIP Only) ===
    private String apiKey;  // Generated on first VIP upgrade
    
    @Builder.Default
    private List<String> webhookUrls = new ArrayList<>();
    
    @Builder.Default
    private Integer rateLimitTier = 1;  // REGULAR: 1, VIP: 5
    
    @Builder.Default
    private Boolean apiAccessEnabled = false;  // VIP only
    
    // === Metadata ===
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Nested class for price alerts
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceAlert {
        private String symbol;
        private String condition;  // above, below
        private Double targetPrice;
        private Boolean enabled;
    }
    
    // Helper methods
    public void applyTierRestrictions(SubscriptionType subscriptionType) {
        if (subscriptionType == SubscriptionType.REGULAR) {
            this.maxIndicators = 3;
            this.rateLimitTier = 1;
            this.apiAccessEnabled = false;
            this.apiKey = null;
            this.webhookUrls.clear();
            
            // Limit favorites to 10 for REGULAR users
            if (this.favoriteSymbols.size() > 10) {
                this.favoriteSymbols = this.favoriteSymbols.subList(0, 10);
            }
            
            // Limit enabled indicators to 3
            if (this.enabledIndicators.size() > 3) {
                this.enabledIndicators = this.enabledIndicators.subList(0, 3);
            }
        } else if (subscriptionType == SubscriptionType.VIP) {
            this.maxIndicators = Integer.MAX_VALUE;
            this.rateLimitTier = 5;
            this.apiAccessEnabled = true;
            
            // Max 50 favorites even for VIP
            if (this.favoriteSymbols.size() > 50) {
                this.favoriteSymbols = this.favoriteSymbols.subList(0, 50);
            }
        }
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}
