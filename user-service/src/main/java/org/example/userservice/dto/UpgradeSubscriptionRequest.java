package org.example.userservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.userservice.model.SubscriptionType;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpgradeSubscriptionRequest {
    @NotNull(message = "Target subscription type is required")
    private SubscriptionType targetType;

    @Min(value = 1, message = "Minimum duration is 1 month")
    @Max(value = 12, message = "Maximum duration is 12 months")
    private int durationMonths = 1;

    private String promoCode; // Optional for future use
}
