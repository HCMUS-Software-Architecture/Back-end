package org.example.userservice;

import org.example.userservice.model.Role;
import org.example.userservice.model.SubscriptionType;
import org.example.userservice.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for subscription and role logic.
 * These tests do NOT require Spring context or MongoDB.
 */
class UserServiceApplicationTests {

    @Nested
    @DisplayName("SubscriptionType Tests")
    class SubscriptionTypeTests {

        @Test
        @DisplayName("REGULAR tier has $0 monthly price")
        void testRegularTierPrice() {
            assertEquals(0.0, SubscriptionType.REGULAR.getMonthlyPrice());
            assertEquals("Regular", SubscriptionType.REGULAR.getDisplayName());
        }

        @Test
        @DisplayName("VIP tier has $29.99 monthly price")
        void testVipTierPrice() {
            assertEquals(29.99, SubscriptionType.VIP.getMonthlyPrice());
            assertEquals("VIP", SubscriptionType.VIP.getDisplayName());
        }
    }

    @Nested
    @DisplayName("Role Tests")
    class RoleTests {

        @Test
        @DisplayName("Role authorities are correct")
        void testRoleAuthorities() {
            assertEquals("ROLE_USER", Role.USER.getAuthority());
            assertEquals("ROLE_VIP", Role.VIP.getAuthority());
        }
    }

    @Nested
    @DisplayName("User Subscription Tests")
    class UserSubscriptionTests {

        @Test
        @DisplayName("hasActiveVipSubscription returns false for REGULAR users")
        void testHasActiveVipSubscription_Regular() {
            User user = User.builder()
                    .subscriptionType(SubscriptionType.REGULAR)
                    .build();
            assertFalse(user.hasActiveVipSubscription());
        }

        @Test
        @DisplayName("hasActiveVipSubscription returns true for VIP with future end date")
        void testHasActiveVipSubscription_VipWithFutureEndDate() {
            User user = User.builder()
                    .subscriptionType(SubscriptionType.VIP)
                    .subscriptionEndDate(LocalDateTime.now().plusMonths(1))
                    .build();
            assertTrue(user.hasActiveVipSubscription());
        }

        @Test
        @DisplayName("hasActiveVipSubscription returns false for VIP with expired end date")
        void testHasActiveVipSubscription_VipExpired() {
            User user = User.builder()
                    .subscriptionType(SubscriptionType.VIP)
                    .subscriptionEndDate(LocalDateTime.now().minusDays(1))
                    .build();
            assertFalse(user.hasActiveVipSubscription());
        }

        @Test
        @DisplayName("hasActiveVipSubscription returns true for VIP with null end date (lifetime)")
        void testHasActiveVipSubscription_VipLifetime() {
            User user = User.builder()
                    .subscriptionType(SubscriptionType.VIP)
                    .subscriptionEndDate(null)
                    .build();
            assertTrue(user.hasActiveVipSubscription());
        }

        @Test
        @DisplayName("syncRolesWithSubscription adds USER role for REGULAR")
        void testSyncRolesWithSubscription_Regular() {
            User user = User.builder()
                    .subscriptionType(SubscriptionType.REGULAR)
                    .roles(new HashSet<>())
                    .build();
            user.syncRolesWithSubscription();

            assertTrue(user.getRoles().contains(Role.USER));
            assertFalse(user.getRoles().contains(Role.VIP));
        }

        @Test
        @DisplayName("syncRolesWithSubscription adds USER and VIP roles for active VIP")
        void testSyncRolesWithSubscription_ActiveVip() {
            User user = User.builder()
                    .subscriptionType(SubscriptionType.VIP)
                    .subscriptionEndDate(LocalDateTime.now().plusMonths(1))
                    .roles(new HashSet<>())
                    .build();
            user.syncRolesWithSubscription();

            assertTrue(user.getRoles().contains(Role.USER));
            assertTrue(user.getRoles().contains(Role.VIP));
        }

        @Test
        @DisplayName("syncRolesWithSubscription removes VIP role for expired VIP")
        void testSyncRolesWithSubscription_ExpiredVip() {
            User user = User.builder()
                    .subscriptionType(SubscriptionType.VIP)
                    .subscriptionEndDate(LocalDateTime.now().minusDays(1))
                    .roles(new HashSet<>())
                    .build();
            user.getRoles().add(Role.VIP); // Manually add VIP role
            user.syncRolesWithSubscription();

            assertTrue(user.getRoles().contains(Role.USER));
            assertFalse(user.getRoles().contains(Role.VIP));
        }

        @Test
        @DisplayName("isOAuthUser returns false for email/password users")
        void testIsOAuthUser_EmailPassword() {
            User user = User.builder()
                    .oauthProvider(null)
                    .build();
            assertFalse(user.isOAuthUser());
        }

        @Test
        @DisplayName("isOAuthUser returns true for Google OAuth users")
        void testIsOAuthUser_Google() {
            User user = User.builder()
                    .oauthProvider("google")
                    .build();
            assertTrue(user.isOAuthUser());
        }

        @Test
        @DisplayName("User builder sets correct defaults")
        void testUserBuilderDefaults() {
            User user = User.builder()
                    .email("test@example.com")
                    .fullName("Test User")
                    .password("password")
                    .build();

            assertEquals(SubscriptionType.REGULAR, user.getSubscriptionType());
            assertFalse(user.getEmailVerified());
            assertTrue(user.getIsActive());
            assertNotNull(user.getRoles());
        }
    }
}
