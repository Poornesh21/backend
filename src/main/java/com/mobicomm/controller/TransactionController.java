package com.mobicomm.controller;

import com.mobicomm.dto.RechargeRequest;
import com.mobicomm.dto.TransactionDto;
import com.mobicomm.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*", 
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS},
        allowedHeaders = "*")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    /**
     * Create a new recharge transaction
     */
    @PostMapping("/recharge")
    public ResponseEntity<TransactionDto> createRechargeTransaction(@RequestBody RechargeRequest rechargeRequest) {
        return ResponseEntity.ok(transactionService.processRecharge(rechargeRequest));
    }

    /**
     * Get transaction history for a mobile number
     */
    @GetMapping("/mobile/{mobileNumber}")
    public ResponseEntity<List<TransactionDto>> getTransactionsByMobileNumber(@PathVariable String mobileNumber) {
        return ResponseEntity.ok(transactionService.getTransactionsByMobileNumber(mobileNumber));
    }
    
    /**
     * Get a specific transaction by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransactionDto> getTransactionById(@PathVariable Integer id) {
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }
    
    /**
     * Get a specific transaction by payment reference (txnId)
     * Note: In the current entity structure, this will be stored in paymentMethod field
     */
    @GetMapping("/reference/{reference}")
    public ResponseEntity<TransactionDto> getTransactionByReference(@PathVariable String reference) {
        TransactionDto transaction = transactionService.getTransactionByReference(reference);
        if (transaction != null) {
            return ResponseEntity.ok(transaction);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}