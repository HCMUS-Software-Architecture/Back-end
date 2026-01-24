package org.example.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.dto.SubscriptionDto;
import org.example.userservice.dto.UpgradeSubscriptionRequest;
import org.example.userservice.model.Role;
import org.example.userservice.model.SubscriptionType;
import org.example.userservice.model.User;
import org.example.userservice.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {
    private final UserRepository userRepository;
    private final UserSettingsService userSettingsService;

    /**
     * Get current subscription status for a user
     */
    public SubscriptionDto getCurrentSubscription(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));

        return buildSubscriptionDto(user);
    }

    /**
     * Get all available subscription tiers
     */
    public List<SubscriptionDto> getAvailableTiers() {
        return Arrays.stream(SubscriptionType.values())
                .map(type -> SubscriptionDto.builder()
                        .type(type)
                        .displayName(type.getDisplayName())
                        .monthlyPrice(type.getMonthlyPrice())
                        .description(type.getDescription())
                        .isActive(true)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Upgrade user to VIP subscription
     */
    @Transactional
    public SubscriptionDto upgradeToVip(String userId, UpgradeSubscriptionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));

        if (request.getTargetType() != SubscriptionType.VIP) {
            throw new IllegalArgumentException("Can only upgrade to VIP. Use cancel endpoint for downgrade.");
        }

        // If already VIP, extend the subscription
        LocalDateTime startDate;
        LocalDateTime endDate;

        if (user.hasActiveVipSubscription() && user.getSubscriptionEndDate() != null) {
            // Extend from current end date
            startDate = user.getSubscriptionStartDate();
            endDate = user.getSubscriptionEndDate().plusMonths(request.getDurationMonths());
            log.info("Extending VIP subscription for user {} by {} months", userId, request.getDurationMonths());
        } else {
            // New subscription
            startDate = LocalDateTime.now();
            endDate = startDate.plusMonths(request.getDurationMonths());
            log.info("New VIP subscription for user {} for {} months", userId, request.getDurationMonths());
        }

        user.setSubscriptionType(SubscriptionType.VIP);
        user.setSubscriptionStartDate(startDate);
        user.setSubscriptionEndDate(endDate);

        // Sync roles with subscription
        user.syncRolesWithSubscription();

        userRepository.save(user);

        // Update user settings for VIP tier (API key, increased limits)
        userSettingsService.handleSubscriptionChange(userId, SubscriptionType.VIP);

        // TODO: Mock payment processing here
        log.info("Mock payment processed: ${} for {} months VIP",
                SubscriptionType.VIP.getMonthlyPrice() * request.getDurationMonths(),
                request.getDurationMonths());

        return buildSubscriptionDto(user);
    }

    /**
     * Renew VIP subscription (extend from current end date)
     */
    @Transactional
    public SubscriptionDto renewSubscription(String userId, int months) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));

        if (user.getSubscriptionType() != SubscriptionType.VIP) {
            throw new IllegalStateException("Cannot renew non-VIP subscription. Use upgrade instead.");
        }

        LocalDateTime newEndDate;
        if (user.getSubscriptionEndDate() != null && user.getSubscriptionEndDate().isAfter(LocalDateTime.now())) {
            // Extend from current end date
            newEndDate = user.getSubscriptionEndDate().plusMonths(months);
        } else {
            // Expired, start fresh
            user.setSubscriptionStartDate(LocalDateTime.now());
            newEndDate = LocalDateTime.now().plusMonths(months);
        }

        user.setSubscriptionEndDate(newEndDate);
        user.syncRolesWithSubscription();
        userRepository.save(user);

        log.info("Renewed VIP subscription for user {} until {}", userId, newEndDate);

        return buildSubscriptionDto(user);
    }

    /**
     * Cancel VIP subscription (downgrade to REGULAR)
     */
    @Transactional
    public void cancelVipSubscription(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));

        if (user.getSubscriptionType() == SubscriptionType.REGULAR) {
            log.warn("User {} is already on REGULAR tier, nothing to cancel", userId);
            return;
        }

        user.setSubscriptionType(SubscriptionType.REGULAR);
        user.setSubscriptionStartDate(null);
        user.setSubscriptionEndDate(null);

        // Sync roles - remove VIP role
        user.syncRolesWithSubscription();

        userRepository.save(user);

        // Update user settings for REGULAR tier (remove API key, apply limits)
        userSettingsService.handleSubscriptionChange(userId, SubscriptionType.REGULAR);

        log.info("Cancelled VIP subscription for user {}, downgraded to REGULAR", userId);
    }

    /**
     * Check if user has VIP access (helper method for other services)
     */
    public boolean hasVipAccess(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));
        return user.hasActiveVipSubscription();
    }

    /**
     * Build SubscriptionDto from User entity
     */
    private SubscriptionDto buildSubscriptionDto(User user) {
        SubscriptionType type = user.getSubscriptionType();
        Integer daysRemaining = calculateDaysRemaining(user);
        boolean isActive = type == SubscriptionType.REGULAR || user.hasActiveVipSubscription();

        return SubscriptionDto.builder()
                .type(type)
                .displayName(type.getDisplayName())
                .monthlyPrice(type.getMonthlyPrice())
                .description(type.getDescription())
                .startDate(user.getSubscriptionStartDate())
                .endDate(user.getSubscriptionEndDate())
                .daysRemaining(daysRemaining)
                .isActive(isActive)
                .build();
    }

    /**
     * Calculate days remaining in subscription
     * Returns -1 for lifetime/REGULAR, 0+ for countdown
     */
    private Integer calculateDaysRemaining(User user) {
        if (user.getSubscriptionType() == SubscriptionType.REGULAR) {
            return -1; // REGULAR has no expiry
        }
        if (user.getSubscriptionEndDate() == null) {
            return -1; // Lifetime VIP
        }
        long days = ChronoUnit.DAYS.between(LocalDateTime.now(), user.getSubscriptionEndDate());
        return (int) Math.max(0, days);
    }
}
