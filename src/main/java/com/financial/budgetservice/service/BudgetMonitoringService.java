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
import java.math.RoundingMode;
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

    /**
     * FIX: Completely rewritten.
     *
     * Previous bug: passed null as userId to findByUserIdAndYearAndMonth() which
     * caused a NullPointerException or silently returned no results.
     *
     * New logic:
     *  1. Load ALL current-month budgets (no userId filter needed here).
     *  2. Group them by userId.
     *  3. For each userId, fetch only their COMPLETED transactions from Transaction-Service.
     *  4. Sum spending per category and update each matching budget's currentSpending.
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Transactional
    public void updateBudgetSpending() {
        log.info("Starting budget spending update...");

        LocalDate now = LocalDate.now();

        // Step 1: Get all budgets for the current month — no userId = null bug
        List<Budget> currentBudgets = budgetRepository.findAll().stream()
                .filter(b -> b.getYear() == now.getYear() && b.getMonth() == now.getMonthValue())
                .collect(Collectors.toList());

        if (currentBudgets.isEmpty()) {
            log.info("No budgets found for {}/{}", now.getMonthValue(), now.getYear());
            return;
        }

        // Step 2: Group budgets by userId so we make one Feign call per user
        Map<Long, List<Budget>> budgetsByUser = currentBudgets.stream()
                .collect(Collectors.groupingBy(Budget::getUserId));

        // Step 3: For each user, fetch their transactions and recalculate spending
        for (Map.Entry<Long, List<Budget>> entry : budgetsByUser.entrySet()) {
            Long userId = entry.getKey();
            List<Budget> userBudgets = entry.getValue();

            List<TransactionDto> transactions;
            try {
                transactions = transactionServiceClient.getTransactionsByUser(userId);
            } catch (Exception e) {
                log.error("Failed to fetch transactions for userId={}: {}", userId, e.getMessage());
                continue; // skip this user but process others
            }

            // Keep only COMPLETED transactions in the current month
            List<TransactionDto> thisMonth = transactions.stream()
                    .filter(t -> "COMPLETED".equals(t.getStatus()))
                    .filter(t -> t.getCreatedAt() != null
                            && t.getCreatedAt().getYear() == now.getYear()
                            && t.getCreatedAt().getMonthValue() == now.getMonthValue())
                    .collect(Collectors.toList());

            // Sum spending per category
            Map<String, BigDecimal> spendingByCategory = thisMonth.stream()
                    .collect(Collectors.groupingBy(
                            t -> t.getCategory() != null ? t.getCategory().toUpperCase() : "TRANSFER",
                            Collectors.reducing(BigDecimal.ZERO, TransactionDto::getAmount, BigDecimal::add)
                    ));

            // Update each budget's currentSpending
            for (Budget budget : userBudgets) {
                BigDecimal spent = spendingByCategory.getOrDefault(
                        budget.getCategory().toUpperCase(), BigDecimal.ZERO);
                budget.setCurrentSpending(spent);
                budgetRepository.save(budget);
                log.info("Updated budget id={} userId={} category={} spending={}/{}",
                        budget.getId(), userId, budget.getCategory(), spent, budget.getMonthlyLimit());
            }
        }

        log.info("Budget spending update complete. Processed {} users.", budgetsByUser.size());
    }

    /**
     * Check all budgets and fire a notification for any that have hit their alert threshold.
     * Runs every minute. No changes needed here — the query in BudgetRepository is correct.
     */
    @Scheduled(fixedRate = 60000)
    public void checkBudgetAlerts() {
        List<Budget> exceededBudgets = budgetRepository.findBudgetsExceedingThreshold();

        if (exceededBudgets.isEmpty()) return;

        log.info("Found {} budget(s) at or above alert threshold", exceededBudgets.size());

        for (Budget budget : exceededBudgets) {
            BigDecimal pct = budget.getCurrentSpending()
                    .divide(budget.getMonthlyLimit(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));

            NotificationRequest request = NotificationRequest.builder()
                    .userId(budget.getUserId())
                    .type("BUDGET_ALERT")
                    .message(String.format(
                            "Alert: You have used %.1f%% of your %s budget for %02d/%d (spent %.2f / limit %.2f)",
                            pct, budget.getCategory(),
                            budget.getMonth(), budget.getYear(),
                            budget.getCurrentSpending(), budget.getMonthlyLimit()))
                    .channel("EMAIL")
                    .build();

            try {
                notificationServiceClient.sendNotification(request);
                log.info("Budget alert sent → userId={} category={} pct={}%",
                        budget.getUserId(), budget.getCategory(), pct);
            } catch (Exception e) {
                log.error("Failed to send notification for budget id={}: {}",
                        budget.getId(), e.getMessage());
            }
        }
    }
}