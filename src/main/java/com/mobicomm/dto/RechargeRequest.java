package com.mobicomm.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RechargeRequest {
    private String mobileNumber;
    private Integer planId;
    private BigDecimal amount;
    private String paymentMethod;
    private String paymentStatus;
    private LocalDateTime transactionDate;
    private LocalDateTime expiryDate;
    private String email; // New field for email address
}