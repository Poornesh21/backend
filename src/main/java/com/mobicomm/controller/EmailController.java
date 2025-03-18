package com.mobicomm.controller;

import com.mobicomm.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/email")
@CrossOrigin(origins = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS},
        allowedHeaders = "*")
public class EmailController {

    @Autowired
    private EmailService emailService;

    /**
     * Send invoice email
     */
    @PostMapping("/send-invoice")
    public ResponseEntity<Map<String, String>> sendInvoiceEmail(@RequestBody InvoiceEmailRequest request) {
        boolean sent = emailService.sendInvoiceEmail(
                request.getEmail(),
                request.getMobileNumber(),
                request.getPlanName(),
                request.getAmount(),
                request.getTransactionId(),
                request.getPaymentMethod(),
                request.getTransactionDate()
        );

        if (sent) {
            return ResponseEntity.ok(Map.of("message", "Invoice email sent successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to send invoice email"));
        }
    }

    /**
     * Simple DTO for invoice email request
     */
    public static class InvoiceEmailRequest {
        private String email;
        private String mobileNumber;
        private String planName;
        private String amount;
        private String transactionId;
        private String paymentMethod;
        private String transactionDate;

        // Getters and setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getMobileNumber() { return mobileNumber; }
        public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }
        
        public String getPlanName() { return planName; }
        public void setPlanName(String planName) { this.planName = planName; }
        
        public String getAmount() { return amount; }
        public void setAmount(String amount) { this.amount = amount; }
        
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        
        public String getTransactionDate() { return transactionDate; }
        public void setTransactionDate(String transactionDate) { this.transactionDate = transactionDate; }
    }
}