package org.example.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.userservice.model.SubscriptionType;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDto {
    private String id;
    private String fullName;
    private String email;
    private SubscriptionType subscriptionType;
    private Boolean emailVerified;
    private LocalDateTime createdAt;
    private String phone;
    private String country;
}
