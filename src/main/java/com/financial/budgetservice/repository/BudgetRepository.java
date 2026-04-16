package com.financial.budgetservice.repository;

import com.financial.budgetservice.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserId(Long userId);
    List<Budget> findByUserIdAndYearAndMonth(Long userId, Integer year, Integer month);
    Optional<Budget> findByUserIdAndCategoryAndYearAndMonth(Long userId, String category, Integer year, Integer month);

    @Query("SELECT b FROM Budget b WHERE b.currentSpending >= (b.monthlyLimit * b.alertThreshold)")
    List<Budget> findBudgetsExceedingThreshold();
}