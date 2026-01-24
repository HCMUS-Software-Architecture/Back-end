package org.example.userservice.model;

/**
 * User roles for Role-Based Access Control (RBAC).
 * USER: Standard user role (all registered users)
 * VIP: Premium user role (active VIP subscription)
 */
public enum Role {
    USER("ROLE_USER", "Regular user"),
    VIP("ROLE_VIP", "VIP subscriber with premium access");

    private final String authority;
    private final String description;

    Role(String authority, String description) {
        this.authority = authority;
        this.description = description;
    }

    public String getAuthority() {
        return authority;
    }

    public String getDescription() {
        return description;
    }
}
