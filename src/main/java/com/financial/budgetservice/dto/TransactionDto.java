package com.financial.budgetservice.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionDto {
    private Long id;
    private Long fromAccountId;
    private Long toAccountId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;
    // We'll need to map category via account type or separate metadata
    // For simplicity, we'll assume transaction category is derived from account type
}