package com.financial.budgetservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationRequest {
    private Long userId;
    private String type; // "BUDGET_ALERT"
    private String message;
    private String channel; // "EMAIL", "SMS"
}