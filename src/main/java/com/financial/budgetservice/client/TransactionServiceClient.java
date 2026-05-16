package com.financial.budgetservice.client;

import com.financial.budgetservice.dto.TransactionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "transaction-service", url = "${transaction.service.url:http://transaction-service:8082}")
public interface TransactionServiceClient {

    // Used by BudgetMonitoringService for full sync (fallback)
    @GetMapping("/api/transactions")
    List<TransactionDto> getAllTransactions();

    // FIX: Per-user endpoint — much more efficient than fetching all transactions
    @GetMapping("/api/transactions/user/{userId}")
    List<TransactionDto> getTransactionsByUser(@PathVariable("userId") Long userId);
}