package com.financial.budgetservice.service;

import com.financial.budgetservice.client.NotificationServiceClient;
import com.financial.budgetservice.client.TransactionServiceClient;
import com.financial.budgetservice.dto.NotificationRequest;
import com.financial.budgetservice.dto.TransactionDto;
import com.financial.budgetservice.entity.Budget;
import com.financial.budgetservice.repository.BudgetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetMonitoringService {

    private final BudgetRepository budgetRepository;
    private final TransactionServiceClient transactionServiceClient;
    private final NotificationServiceClient notificationServiceClient;

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Transactional
    public void updateBudgetSpending() {
        log.info("Updating budget spending...");

        // Fetch all transactions (in real scenario, only recent ones)
        List<TransactionDto> transactions;
        try {
            transactions = transactionServiceClient.getAllTransactions();
        } catch (Exception e) {
            log.error("Failed to fetch transactions: {}", e.getMessage());
            return;
        }

        // Group transactions by account and category (simplified)
        // We need a mapping from accountId to userId and category.
        // For now, we'll assume we have a way to get this via Account Service.
        // Let's simplify: fetch budgets for current month and update based on all transactions.
        LocalDate now = LocalDate.now();
        List<Budget> currentBudgets = budgetRepository.findByUserIdAndYearAndMonth(
                // This is placeholder; in real code you'd iterate all users.
                // For demo, we'll use a fixed userId or fetch all budgets.
                null, now.getYear(), now.getMonthValue()
        );

        // In a real system, you'd call Account Service to get account details including userId
        // Here we'll just log a warning and skip.
        log.warn("Full budget monitoring requires integration with Account Service. Implement mapping logic.");
    }

    @Scheduled(fixedRate = 60000) // Every minute check thresholds
    public void checkBudgetAlerts() {
        List<Budget> exceededBudgets = budgetRepository.findBudgetsExceedingThreshold();
        for (Budget budget : exceededBudgets) {
            NotificationRequest request = NotificationRequest.builder()
                    .userId(budget.getUserId())
                    .type("BUDGET_ALERT")
                    .message(String.format("You have used %.2f%% of your %s budget for %d/%d",
                            budget.getCurrentSpending().divide(budget.getMonthlyLimit(), 2, BigDecimal.ROUND_HALF_UP)
                                    .multiply(new BigDecimal("100")),
                            budget.getCategory(),
                            budget.getMonth(),
                            budget.getYear()))
                    .channel("EMAIL")
                    .build();
            try {
                notificationServiceClient.sendNotification(request);
                log.info("Alert sent for budget ID: {}", budget.getId());
            } catch (Exception e) {
                log.error("Failed to send notification: {}", e.getMessage());
            }
        }
    }
}