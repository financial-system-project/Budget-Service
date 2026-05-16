package com.financial.budgetservice.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionDto {
    private Long id;
    private Long fromAccountId;
    private Long toAccountId;

    // FIX: Now populated by Transaction-Service after the entity was updated
    private Long userId;

    // FIX: Category field — matches budget categories (e.g. "FOOD", "RENT", "TRANSFER")
    private String category;

    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}