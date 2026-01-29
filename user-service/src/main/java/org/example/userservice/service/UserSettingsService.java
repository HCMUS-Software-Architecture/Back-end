package org.example.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.dto.UpdateUserSettingsDto;
import org.example.userservice.dto.UserSettingsDto;
import org.example.userservice.exception.ResourceNotFoundException;
import org.example.userservice.model.SubscriptionType;
import org.example.userservice.model.User;
import org.example.userservice.model.UserSettings;
import org.example.userservice.repository.UserRepository;
import org.example.userservice.repository.UserSettingsRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;
    private final UserRepository userRepository;
    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * Get user settings with Redis caching
     * Cache key: userId
     * TTL: 1 hour (configured in RedisConfig)
     */
    @Cacheable(value = "userSettings", key = "#userId")
    public UserSettingsDto getSettings(String userId) {
        log.info("Fetching settings for user: {} (cache miss)", userId);

        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));

        return mapToDto(settings);
    }

    /**
     * Update user settings and update cache
     * Cache key: userId
     */
    @CachePut(value = "userSettings", key = "#userId")
    @Transactional
    public UserSettingsDto updateSettings(String userId, UpdateUserSettingsDto updateDto) {
        log.info("Updating settings for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));

        // Update chart preferences
        if (updateDto.getDefaultTimeframe() != null) {
            settings.setDefaultTimeframe(updateDto.getDefaultTimeframe());
        }
        if (updateDto.getDefaultSymbol() != null) {
            settings.setDefaultSymbol(updateDto.getDefaultSymbol());
        }
        if (updateDto.getChartTheme() != null) {
            settings.setChartTheme(updateDto.getChartTheme());
        }
        if (updateDto.getFavoriteSymbols() != null) {
            settings.setFavoriteSymbols(updateDto.getFavoriteSymbols());
        }
        if (updateDto.getEnabledIndicators() != null) {
            settings.setEnabledIndicators(updateDto.getEnabledIndicators());
        }
        if (updateDto.getShowVolume() != null) {
            settings.setShowVolume(updateDto.getShowVolume());
        }
        if (updateDto.getShowGrid() != null) {
            settings.setShowGrid(updateDto.getShowGrid());
        }

        // Update notification preferences
        if (updateDto.getEmailNotifications() != null) {
            settings.setEmailNotifications(updateDto.getEmailNotifications());
        }
        if (updateDto.getPushNotifications() != null) {
            settings.setPushNotifications(updateDto.getPushNotifications());
        }
        if (updateDto.getPriceAlerts() != null) {
            settings.setPriceAlerts(updateDto.getPriceAlerts());
        }
        if (updateDto.getPriceAlertList() != null) {
            settings.setPriceAlertList(
                    updateDto.getPriceAlertList().stream()
                            .map(dto -> UserSettings.PriceAlert.builder()
                                    .symbol(dto.getSymbol())
                                    .condition(dto.getCondition())
                                    .targetPrice(dto.getTargetPrice())
                                    .enabled(dto.getEnabled())
                                    .build())
                            .collect(Collectors.toList()));
        }
        if (updateDto.getNewsNotifications() != null) {
            settings.setNewsNotifications(updateDto.getNewsNotifications());
        }
        if (updateDto.getNewsTopics() != null) {
            settings.setNewsTopics(updateDto.getNewsTopics());
        }

        // Update display preferences
        if (updateDto.getLanguage() != null) {
            settings.setLanguage(updateDto.getLanguage());
        }
        if (updateDto.getTimezone() != null) {
            settings.setTimezone(updateDto.getTimezone());
        }
        if (updateDto.getDateFormat() != null) {
            settings.setDateFormat(updateDto.getDateFormat());
        }
        if (updateDto.getCurrencyDisplay() != null) {
            settings.setCurrencyDisplay(updateDto.getCurrencyDisplay());
        }
        if (updateDto.getUse24HourFormat() != null) {
            settings.setUse24HourFormat(updateDto.getUse24HourFormat());
        }

        // Update privacy settings
        if (updateDto.getProfileVisible() != null) {
            settings.setProfileVisible(updateDto.getProfileVisible());
        }
        if (updateDto.getShareAnalytics() != null) {
            settings.setShareAnalytics(updateDto.getShareAnalytics());
        }
        if (updateDto.getShowOnlineStatus() != null) {
            settings.setShowOnlineStatus(updateDto.getShowOnlineStatus());
        }
        if (updateDto.getAllowMarketingEmails() != null) {
            settings.setAllowMarketingEmails(updateDto.getAllowMarketingEmails());
        }

        // Update webhook URLs (VIP only)
        if (updateDto.getWebhookUrls() != null && user.getSubscriptionType() == SubscriptionType.VIP) {
            settings.setWebhookUrls(updateDto.getWebhookUrls());
        }

        // Apply tier restrictions based on current subscription
        settings.applyTierRestrictions(user.getSubscriptionType());
        settings.updateTimestamp();

        UserSettings saved = userSettingsRepository.save(settings);
        log.info("Settings updated and cached for user: {}", userId);

        return mapToDto(saved);
    }

    /**
     * Reset settings to defaults and evict from cache
     */
    @CacheEvict(value = "userSettings", key = "#userId")
    @Transactional
    public UserSettingsDto resetToDefaults(String userId) {
        log.info("Resetting settings to defaults for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        userSettingsRepository.deleteByUserId(userId);
        UserSettings newSettings = createDefaultSettings(userId);
        newSettings.applyTierRestrictions(user.getSubscriptionType());

        UserSettings saved = userSettingsRepository.save(newSettings);
        log.info("Settings reset and cache evicted for user: {}", userId);

        return mapToDto(saved);
    }

    /**
     * Handle subscription tier changes (called from SubscriptionService)
     * Updates settings restrictions and regenerates API key if upgraded to VIP
     */
    @CachePut(value = "userSettings", key = "#userId")
    @Transactional
    public UserSettingsDto handleSubscriptionChange(String userId, SubscriptionType newType) {
        log.info("Handling subscription change for user: {} to {}", userId, newType);

        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));

        // Generate API key if upgrading to VIP for the first time
        if (newType == SubscriptionType.VIP && settings.getApiKey() == null) {
            settings.setApiKey(generateApiKey());
            log.info("API key generated for new VIP user: {}", userId);
        }

        // Clear API key if downgrading from VIP
        if (newType == SubscriptionType.REGULAR && settings.getApiKey() != null) {
            log.info("API key revoked for downgraded user: {}", userId);
        }

        settings.applyTierRestrictions(newType);
        UserSettings saved = userSettingsRepository.save(settings);

        log.info("Settings updated for subscription change, cache refreshed for user: {}", userId);
        return mapToDto(saved);
    }

    /**
     * Delete user settings and evict from cache (called on account deletion)
     */
    @CacheEvict(value = "userSettings", key = "#userId")
    @Transactional
    public void deleteSettings(String userId) {
        log.info("Deleting settings for user: {}", userId);
        userSettingsRepository.deleteByUserId(userId);
    }

    /**
     * Regenerate API key (VIP users only)
     */
    @CachePut(value = "userSettings", key = "#userId")
    @Transactional
    public UserSettingsDto regenerateApiKey(String userId) {
        log.info("Regenerating API key for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getSubscriptionType() != SubscriptionType.VIP) {
            throw new IllegalStateException("API key regeneration is only available for VIP users");
        }

        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));

        settings.setApiKey(generateApiKey());
        settings.updateTimestamp();

        UserSettings saved = userSettingsRepository.save(settings);
        log.info("API key regenerated and cached for user: {}", userId);

        return mapToDto(saved);
    }

    // === Helper Methods ===

    private UserSettings createDefaultSettings(String userId) {
        log.info("Creating default settings for user: {}", userId);
        return UserSettings.builder()
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private String generateApiKey() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return "tp_" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private UserSettingsDto mapToDto(UserSettings settings) {
        return UserSettingsDto.builder()
                .id(settings.getId())
                .userId(settings.getUserId())
                .defaultTimeframe(settings.getDefaultTimeframe())
                .defaultSymbol(settings.getDefaultSymbol())
                .chartTheme(settings.getChartTheme())
                .favoriteSymbols(settings.getFavoriteSymbols())
                .enabledIndicators(settings.getEnabledIndicators())
                .maxIndicators(settings.getMaxIndicators())
                .showVolume(settings.getShowVolume())
                .showGrid(settings.getShowGrid())
                .emailNotifications(settings.getEmailNotifications())
                .pushNotifications(settings.getPushNotifications())
                .priceAlerts(settings.getPriceAlerts())
                .priceAlertList(
                        settings.getPriceAlertList().stream()
                                .map(alert -> UserSettingsDto.PriceAlertDto.builder()
                                        .symbol(alert.getSymbol())
                                        .condition(alert.getCondition())
                                        .targetPrice(alert.getTargetPrice())
                                        .enabled(alert.getEnabled())
                                        .build())
                                .collect(Collectors.toList()))
                .newsNotifications(settings.getNewsNotifications())
                .newsTopics(settings.getNewsTopics())
                .language(settings.getLanguage())
                .timezone(settings.getTimezone())
                .dateFormat(settings.getDateFormat())
                .currencyDisplay(settings.getCurrencyDisplay())
                .use24HourFormat(settings.getUse24HourFormat())
                .profileVisible(settings.getProfileVisible())
                .shareAnalytics(settings.getShareAnalytics())
                .showOnlineStatus(settings.getShowOnlineStatus())
                .allowMarketingEmails(settings.getAllowMarketingEmails())
                .apiKey(settings.getApiKey())
                .webhookUrls(settings.getWebhookUrls())
                .rateLimitTier(settings.getRateLimitTier())
                .apiAccessEnabled(settings.getApiAccessEnabled())
                .createdAt(settings.getCreatedAt())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
}
