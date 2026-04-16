package com.financial.budgetservice.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BudgetRequest {
    private Long userId;
    private String category;
    private BigDecimal monthlyLimit;
    private Integer month;
    private Integer year;
    private BigDecimal alertThreshold;
}