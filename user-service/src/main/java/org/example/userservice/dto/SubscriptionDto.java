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
public class SubscriptionDto {
    private SubscriptionType type;
    private String displayName;
    private double monthlyPrice;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer daysRemaining; // -1 for lifetime, 0+ for countdown
    private boolean isActive;
}
