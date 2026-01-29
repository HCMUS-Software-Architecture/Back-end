package org.example.userservice.model;

/**
 * Subscription tiers for the trading platform.
 * REGULAR: Free tier with basic features
 * VIP: Premium tier with advanced features ($29.99/month)
 */
public enum SubscriptionType {
    REGULAR("Regular", 0.0, "Basic trading features + Real-time prices"),
    VIP("VIP", 29.99, "Advanced analytics, Priority support, Extended history");

    private final String displayName;
    private final double monthlyPrice;
    private final String description;

    SubscriptionType(String displayName, double monthlyPrice, String description) {
        this.displayName = displayName;
        this.monthlyPrice = monthlyPrice;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getMonthlyPrice() {
        return monthlyPrice;
    }

    public String getDescription() {
        return description;
    }
}
