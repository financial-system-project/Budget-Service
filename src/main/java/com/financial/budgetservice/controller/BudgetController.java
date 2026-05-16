package com.financial.budgetservice.controller;

import com.financial.budgetservice.dto.BudgetRequest;
import com.financial.budgetservice.entity.Budget;
import com.financial.budgetservice.repository.BudgetRepository;
import com.financial.budgetservice.service.BudgetMonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/budgets")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetRepository budgetRepository;
    private final BudgetMonitoringService budgetMonitoringService;

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("service", "budget-service");
        response.put("status", "UP");
        return response;
    }

    @PostMapping
    public ResponseEntity<Budget> createBudget(@RequestBody BudgetRequest request) {
        Optional<Budget> existing = budgetRepository.findByUserIdAndCategoryAndYearAndMonth(
                request.getUserId(), request.getCategory(), request.getYear(), request.getMonth());
        if (existing.isPresent()) {
            return ResponseEntity.badRequest().build();
        }
        Budget budget = Budget.builder()
                .userId(request.getUserId())
                .category(request.getCategory())
                .monthlyLimit(request.getMonthlyLimit())
                .currentSpending(BigDecimal.ZERO)
                .month(request.getMonth())
                .year(request.getYear())
                .alertThreshold(request.getAlertThreshold())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetRepository.save(budget));
    }

    @GetMapping("/user/{userId}")
    public List<Budget> getUserBudgets(@PathVariable Long userId) {
        return budgetRepository.findByUserId(userId);
    }

    @GetMapping("/user/{userId}/month/{year}/{month}")
    public List<Budget> getMonthlyBudgets(@PathVariable Long userId,
                                          @PathVariable Integer year,
                                          @PathVariable Integer month) {
        return budgetRepository.findByUserIdAndYearAndMonth(userId, year, month);
    }

    /** Directly set currentSpending to an absolute value */
    @PostMapping("/{id}/spending")
    public ResponseEntity<Budget> updateSpending(@PathVariable Long id,
                                                 @RequestBody Map<String, BigDecimal> body) {
        return budgetRepository.findById(id).map(budget -> {
            budget.setCurrentSpending(body.get("amount"));
            return ResponseEntity.ok(budgetRepository.save(budget));
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * FIX: Add an amount to currentSpending instead of overwriting it.
     * Call this after every transaction to keep budgets in sync without waiting
     * for the 5-minute scheduler — useful for the live demo.
     *
     * POST /api/budgets/{id}/spending/add   body: {"amount": 50.00}
     */
    @PostMapping("/{id}/spending/add")
    public ResponseEntity<Budget> addSpending(@PathVariable Long id,
                                              @RequestBody Map<String, BigDecimal> body) {
        BigDecimal toAdd = body.get("amount");
        if (toAdd == null || toAdd.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().build();
        }
        return budgetRepository.findById(id).map(budget -> {
            BigDecimal current = budget.getCurrentSpending() != null
                    ? budget.getCurrentSpending() : BigDecimal.ZERO;
            budget.setCurrentSpending(current.add(toAdd));
            Budget saved = budgetRepository.save(budget);
            // Immediately check alert threshold after each spend update
            budgetMonitoringService.checkBudgetAlerts();
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * FIX: Manually trigger the full spending recalculation from Transaction-Service.
     * Handy for the demo — no need to wait 5 minutes.
     *
     * POST /api/budgets/sync
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, String>> syncSpending() {
        budgetMonitoringService.updateBudgetSpending();
        return ResponseEntity.ok(Map.of("status", "sync triggered"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Budget> updateBudget(@PathVariable Long id,
                                               @RequestBody BudgetRequest request) {
        return budgetRepository.findById(id)
                .map(budget -> {
                    budget.setMonthlyLimit(request.getMonthlyLimit());
                    budget.setAlertThreshold(request.getAlertThreshold());
                    return ResponseEntity.ok(budgetRepository.save(budget));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id) {
        budgetRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}