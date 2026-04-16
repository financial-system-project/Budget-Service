package com.financial.budgetservice.controller;

import com.financial.budgetservice.dto.BudgetRequest;
import com.financial.budgetservice.entity.Budget;
import com.financial.budgetservice.repository.BudgetRepository;
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
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetRepository budgetRepository;

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
        Budget saved = budgetRepository.save(budget);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/user/{userId}")
    public List<Budget> getUserBudgets(@PathVariable Long userId) {
        return budgetRepository.findByUserId(userId);
    }

    @GetMapping("/user/{userId}/month/{year}/{month}")
    public List<Budget> getMonthlyBudgets(@PathVariable Long userId, @PathVariable Integer year, @PathVariable Integer month) {
        return budgetRepository.findByUserIdAndYearAndMonth(userId, year, month);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Budget> updateBudget(@PathVariable Long id, @RequestBody BudgetRequest request) {
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