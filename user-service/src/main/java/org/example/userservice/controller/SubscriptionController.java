package org.example.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.dto.SubscriptionDto;
import org.example.userservice.dto.UpgradeSubscriptionRequest;
import org.example.userservice.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Subscription", description = "User subscription management APIs")
public class SubscriptionController {
    private final SubscriptionService subscriptionService;

    private String getCurrentUserId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping("/tiers")
    @Operation(summary = "Get all available subscription tiers")
    public ResponseEntity<List<SubscriptionDto>> getAvailableTiers() {
        return ResponseEntity.ok(subscriptionService.getAvailableTiers());
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user's subscription status")
    public ResponseEntity<SubscriptionDto> getCurrentSubscription() {
        String userId = getCurrentUserId();
        return ResponseEntity.ok(subscriptionService.getCurrentSubscription(userId));
    }

    @PostMapping("/upgrade")
    @Operation(summary = "Upgrade to VIP subscription (mock payment)")
    public ResponseEntity<SubscriptionDto> upgradeSubscription(
            @Valid @RequestBody UpgradeSubscriptionRequest request) {
        String userId = getCurrentUserId();
        log.info("User {} requesting upgrade to {}", userId, request.getTargetType());
        return ResponseEntity.ok(subscriptionService.upgradeToVip(userId, request));
    }

    @PostMapping("/renew")
    @Operation(summary = "Renew current VIP subscription")
    public ResponseEntity<SubscriptionDto> renewSubscription(
            @RequestParam(defaultValue = "1") int months) {
        String userId = getCurrentUserId();
        log.info("User {} requesting renewal for {} months", userId, months);
        return ResponseEntity.ok(subscriptionService.renewSubscription(userId, months));
    }

    @PostMapping("/cancel")
    @Operation(summary = "Cancel VIP subscription (downgrade to REGULAR)")
    public ResponseEntity<Map<String, String>> cancelSubscription() {
        String userId = getCurrentUserId();
        log.info("User {} requesting subscription cancellation", userId);
        subscriptionService.cancelVipSubscription(userId);
        return ResponseEntity.ok(Map.of(
                "message", "Subscription cancelled successfully. You have been downgraded to REGULAR tier."
        ));
    }

    @GetMapping("/check-vip")
    @Operation(summary = "Check if current user has VIP access")
    public ResponseEntity<Map<String, Boolean>> checkVipAccess() {
        String userId = getCurrentUserId();
        boolean hasVip = subscriptionService.hasVipAccess(userId);
        return ResponseEntity.ok(Map.of("hasVipAccess", hasVip));
    }
}
