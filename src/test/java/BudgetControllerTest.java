package com.financial.budgetservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financial.budgetservice.dto.BudgetRequest;
import com.financial.budgetservice.entity.Budget;
import com.financial.budgetservice.repository.BudgetRepository;
import com.financial.budgetservice.service.BudgetMonitoringService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BudgetController.class)
class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BudgetRepository budgetRepository;

    @MockBean
    private BudgetMonitoringService budgetMonitoringService;

    @Test
    @DisplayName("Health endpoint should return UP")
    void health_ShouldReturnUp() throws Exception {

        mockMvc.perform(get("/api/budgets/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("budget-service"))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Create budget should return created budget")
    void createBudget_ShouldReturnCreated() throws Exception {

        BudgetRequest request = new BudgetRequest();
        request.setUserId(1L);
        request.setCategory("FOOD");
        request.setMonthlyLimit(new BigDecimal("300.00"));
        request.setMonth(4);
        request.setYear(2025);
        request.setAlertThreshold(new BigDecimal("80"));

        Budget saved = Budget.builder()
                .id(1L)
                .userId(1L)
                .category("FOOD")
                .monthlyLimit(new BigDecimal("300.00"))
                .currentSpending(BigDecimal.ZERO)
                .month(4)
                .year(2025)
                .alertThreshold(new BigDecimal("80"))
                .build();

        when(budgetRepository.findByUserIdAndCategoryAndYearAndMonth(
                1L, "FOOD", 2025, 4))
                .thenReturn(Optional.empty());

        when(budgetRepository.save(any(Budget.class)))
                .thenReturn(saved);

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.category", is("FOOD")));

        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    @DisplayName("Create budget should return bad request when duplicate exists")
    void createBudget_ShouldReturnBadRequest_WhenDuplicateExists() throws Exception {

        BudgetRequest request = new BudgetRequest();
        request.setUserId(1L);
        request.setCategory("FOOD");
        request.setMonthlyLimit(new BigDecimal("300.00"));
        request.setMonth(4);
        request.setYear(2025);

        when(budgetRepository.findByUserIdAndCategoryAndYearAndMonth(
                1L, "FOOD", 2025, 4))
                .thenReturn(Optional.of(new Budget()));

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Get user budgets should return budget list")
    void getUserBudgets_ShouldReturnList() throws Exception {

        when(budgetRepository.findByUserId(1L))
                .thenReturn(Arrays.asList(
                        Budget.builder()
                                .id(1L)
                                .userId(1L)
                                .category("FOOD")
                                .build(),

                        Budget.builder()
                                .id(2L)
                                .userId(1L)
                                .category("ENTERTAINMENT")
                                .build()
                ));

        mockMvc.perform(get("/api/budgets/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("Update spending should return updated budget")
    void updateSpending_ShouldReturnUpdatedBudget() throws Exception {

        Budget budget = Budget.builder()
                .id(1L)
                .currentSpending(BigDecimal.ZERO)
                .build();

        Budget updated = Budget.builder()
                .id(1L)
                .currentSpending(new BigDecimal("150.00"))
                .build();

        when(budgetRepository.findById(1L))
                .thenReturn(Optional.of(budget));

        when(budgetRepository.save(any(Budget.class)))
                .thenReturn(updated);

        mockMvc.perform(post("/api/budgets/1/spending")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("amount", new BigDecimal("150.00"))
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentSpending", is(150.00)));
    }

    @Test
    @DisplayName("Add spending should increment current spending")
    void addSpending_ShouldIncrementSpending() throws Exception {

        Budget budget = Budget.builder()
                .id(1L)
                .currentSpending(new BigDecimal("100.00"))
                .build();

        Budget updated = Budget.builder()
                .id(1L)
                .currentSpending(new BigDecimal("150.00"))
                .build();

        when(budgetRepository.findById(1L))
                .thenReturn(Optional.of(budget));

        when(budgetRepository.save(any(Budget.class)))
                .thenReturn(updated);

        mockMvc.perform(post("/api/budgets/1/spending/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("amount", new BigDecimal("50.00"))
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentSpending", is(150.00)));

        verify(budgetMonitoringService).checkBudgetAlerts();
    }

    @Test
    @DisplayName("Sync endpoint should trigger spending recalculation")
    void syncSpending_ShouldReturnSuccess() throws Exception {

        mockMvc.perform(post("/api/budgets/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("sync triggered"));

        verify(budgetMonitoringService).updateBudgetSpending();
    }

    @Test
    @DisplayName("Delete budget should return no content")
    void deleteBudget_ShouldReturnNoContent() throws Exception {

        mockMvc.perform(delete("/api/budgets/1"))
                .andExpect(status().isNoContent());
    }
}