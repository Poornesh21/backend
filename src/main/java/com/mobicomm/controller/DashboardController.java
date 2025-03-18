package com.mobicomm.controller;

import com.mobicomm.dto.TransactionDto;
import com.mobicomm.dto.UserExpiringPlanDto;
import com.mobicomm.service.DashboardService;
import com.mobicomm.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private TransactionService transactionService;

    /**
     * Get users with plans expiring within 1-3 days
     * This endpoint specifically focuses on user data with their expiring plans
     */
    @GetMapping("/expiring-plans")
    public ResponseEntity<List<TransactionDto>> getExpiringTransactionsWithin3Days() {
        // Use transaction service to get transactions expiring in 3 days
        List<TransactionDto> expiringTransactions = transactionService.getRecentTransactions(3);
        return ResponseEntity.ok(expiringTransactions);
    }

    /**
     * Get users with plans expiring within 1-3 days
     * This endpoint specifically focuses on user data with their expiring plans
     */
    @GetMapping("/users-expiring-plans")
    public ResponseEntity<List<UserExpiringPlanDto>> getUsersWithExpiringPlans() {
        List<UserExpiringPlanDto> usersWithExpiringPlans = dashboardService.getUsersWithExpiringPlans();
        return ResponseEntity.ok(usersWithExpiringPlans);
    }

    /**
     * Get user transaction history
     */
    @GetMapping("/user-transactions/{userId}")
    public ResponseEntity<List<TransactionDto>> getUserTransactionHistory(
            @PathVariable Integer userId
    ) {
        List<TransactionDto> userTransactions = dashboardService.getUserTransactionHistory(userId);
        return ResponseEntity.ok(userTransactions);
    }

    /**
     * Get upcoming expiring transactions
     */
    @GetMapping("/upcoming-expiring-plans")
    public ResponseEntity<List<TransactionDto>> getAllUpcomingExpiringTransactions() {
        // Use transaction service to get transactions expiring in 30 days
        List<TransactionDto> upcomingExpiringTransactions = transactionService.getRecentTransactions(30);
        return ResponseEntity.ok(upcomingExpiringTransactions);
    }

    /**
     * Get dashboard statistics - Simplified to prevent LazyInitializationException
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getDashboardStatistics() {
        // Build a simplified statistics map to avoid LazyInitializationException
        Map<String, Object> statistics = new HashMap<>();

        // Use transaction service to get comprehensive transaction statistics
        Map<String, Object> transactionStats = transactionService.getTransactionStatistics(null, null);

        // Transfer key statistics to our dashboard stats
        statistics.put("totalRevenue", transactionStats.get("totalRevenue"));
        statistics.put("totalTransactions", transactionStats.get("totalTransactions"));
        statistics.put("successfulTransactions", transactionStats.get("successfulTransactions"));
        statistics.put("failedTransactions", transactionStats.get("failedTransactions"));
        statistics.put("popularPlan", transactionStats.get("popularPlan"));

        return ResponseEntity.ok(statistics);
    }
}