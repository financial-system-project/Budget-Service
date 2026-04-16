package com.financial.budgetservice.client;

import com.financial.budgetservice.dto.TransactionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@FeignClient(name = "transaction-service", url = "${transaction.service.url:http://transaction-service:8080}")
public interface TransactionServiceClient {

    // In a real implementation, you'd have an endpoint to get transactions by account and date range
    // For now, we'll simulate by fetching all transactions (or implement a filtered endpoint)
    @GetMapping("/api/transactions")
    List<TransactionDto> getAllTransactions();
}