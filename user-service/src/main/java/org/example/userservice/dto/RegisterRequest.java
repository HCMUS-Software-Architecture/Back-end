package org.example.userservice.dto;

import lombok.Data;
import org.example.userservice.model.SubscriptionType;

@Data
public class RegisterRequest {
    private String email;
    private String password;
    private String userName;
    private String country;
    private String phone;
    private SubscriptionType subscriptionType; // Optional: defaults to REGULAR if null
}
