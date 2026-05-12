package com.financial.budgetservice.controller;

import com.financial.budgetservice.entity.Budget;
import com.financial.budgetservice.repository.BudgetRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BudgetController.class)
class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BudgetRepository budgetRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void health_ShouldReturnUp() throws Exception {
        mockMvc.perform(get("/api/budgets/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void createBudget_ShouldReturnCreated() throws Exception {
        Budget input = Budget.builder()
                .userId(1L)
                .category("FOOD")
                .monthlyLimit(new BigDecimal("300.00"))
                .currentSpending(BigDecimal.ZERO)
                .month(4)
                .year(2025)
                .build();
        Budget saved = Budget.builder().id(1L).userId(1L).category("FOOD").monthlyLimit(new BigDecimal("300.00")).build();

        when(budgetRepository.findByUserIdAndCategoryAndYearAndMonth(1L, "FOOD", 2025, 4)).thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class))).thenReturn(saved);

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    void getUserBudgets_ShouldReturnList() throws Exception {
        when(budgetRepository.findByUserId(1L)).thenReturn(Arrays.asList(
                Budget.builder().id(1L).userId(1L).category("FOOD").build(),
                Budget.builder().id(2L).userId(1L).category("ENTERTAINMENT").build()
        ));

        mockMvc.perform(get("/api/budgets/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void deleteBudget_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/budgets/1"))
                .andExpect(status().isNoContent());
    }
}