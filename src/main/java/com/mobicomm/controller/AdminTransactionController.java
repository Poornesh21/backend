package com.mobicomm.controller;

import com.mobicomm.dto.TransactionDto;
import com.mobicomm.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Admin Transaction Controller
 * Provides management operations for transactions, restricted to users with ADMIN role
 */
@RestController
@RequestMapping("/api/admin/transactions")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.OPTIONS},
        allowedHeaders = "*")
public class AdminTransactionController {

    @Autowired
    private TransactionService transactionService;

    /**
     * Get all transactions with optional filtering
     */
    @GetMapping
    public ResponseEntity<List<TransactionDto>> getAllTransactions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Integer planId
    ) {
        return ResponseEntity.ok(transactionService.getAllTransactionsWithFilters(status, fromDate, toDate, planId));
    }

    /**
     * Get transaction details by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransactionDto> getTransactionById(@PathVariable Integer id) {
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }

    /**
     * Get transaction statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getTransactionStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ResponseEntity.ok(transactionService.getTransactionStatistics(fromDate, toDate));
    }

    /**
     * Get transactions by mobile number
     */
    @GetMapping("/mobile/{mobileNumber}")
    public ResponseEntity<List<TransactionDto>> getTransactionsByMobileNumber(@PathVariable String mobileNumber) {
        return ResponseEntity.ok(transactionService.getTransactionsByMobileNumber(mobileNumber));
    }

    /**
     * Get transactions by user ID
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TransactionDto>> getTransactionsByUserId(@PathVariable Integer userId) {
        return ResponseEntity.ok(transactionService.getTransactionsByUserId(userId));
    }

    /**
     * Get recent transactions (last 30 days by default)
     */
    @GetMapping("/recent")
    public ResponseEntity<List<TransactionDto>> getRecentTransactions(
            @RequestParam(defaultValue = "30") Integer days
    ) {
        return ResponseEntity.ok(transactionService.getRecentTransactions(days));
    }
}