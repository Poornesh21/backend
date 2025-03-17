package com.mobicomm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionDto {
    private Integer transactionId;
    private Integer userId;
    private String mobileNumber; // Retrieved from User entity
    private Integer planId;
    private String planName;
    private BigDecimal amount;
    private String paymentStatus;
    private String paymentMethod;
    private String paymentReference; // This will be stored in paymentMethod field
    private LocalDateTime transactionDate;
    private LocalDateTime expiryDate;

    // Plan details for convenience
    private String data;
    private String validity;
    private String calls;
    private String sms;
    private String ottBenefits;
}