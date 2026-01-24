package org.example.userservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Document(collection = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {
    @Id
    private String id;

    private String email;
    private String password;
    private String fullName;

    // Subscription fields
    @Builder.Default
    private SubscriptionType subscriptionType = SubscriptionType.REGULAR;
    private LocalDateTime subscriptionStartDate;
    private LocalDateTime subscriptionEndDate; // null = lifetime for REGULAR

    // Role-based access control
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    // Account status
    @Builder.Default
    private Boolean emailVerified = false;
    @Builder.Default
    private Boolean isActive = true;

    // Audit fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // OAuth support (for future Google OAuth)
    private String oauthProvider; // "google", null for email/password users

    /**
     * Check if user has an active VIP subscription.
     * VIP is active if:
     * 1. subscriptionType is VIP AND
     * 2. subscriptionEndDate is null (lifetime) OR subscriptionEndDate is in the
     * future
     */
    public boolean hasActiveVipSubscription() {
        if (subscriptionType != SubscriptionType.VIP) {
            return false;
        }
        // Lifetime VIP (no end date) or future end date
        return subscriptionEndDate == null || subscriptionEndDate.isAfter(LocalDateTime.now());
    }

    /**
     * Sync user roles based on subscription status.
     * All users get USER role. VIP subscribers also get VIP role.
     */
    public void syncRolesWithSubscription() {
        if (roles == null) {
            roles = new HashSet<>();
        }
        // Always ensure USER role
        roles.add(Role.USER);

        // Add or remove VIP role based on subscription
        if (hasActiveVipSubscription()) {
            roles.add(Role.VIP);
        } else {
            roles.remove(Role.VIP);
        }
    }

    /**
     * Check if user registered via OAuth provider.
     */
    public boolean isOAuthUser() {
        return oauthProvider != null && !oauthProvider.isEmpty();
    }
}
