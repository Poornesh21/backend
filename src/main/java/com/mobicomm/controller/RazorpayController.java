package com.mobicomm.controller;

import com.mobicomm.dto.RechargeRequest;
import com.mobicomm.service.RazorpayService;
import com.mobicomm.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/razorpay")
@CrossOrigin(origins = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS},
        allowedHeaders = "*")
public class RazorpayController {

    private static final Logger logger = LoggerFactory.getLogger(RazorpayController.class);

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Autowired
    private RazorpayService razorpayService;

    @Autowired
    private TransactionService transactionService;

    /**
     * Create Razorpay order for a mobile recharge
     */
    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createRazorpayOrder(@RequestBody Map<String, Object> request) {
        try {
            // Log entire request for debugging
            logger.error("Full Request Payload: {}", request);

            // Extract parameters with safe defaults and detailed logging
            String mobileNumber = extractStringParam(request, "mobileNumber", "");
            Integer planId = extractIntegerParam(request, "planId", 1);
            BigDecimal amount = extractBigDecimalParam(request, "amount", BigDecimal.ZERO);
            String email = extractStringParam(request, "email", "");

            // Detailed logging of extracted parameters
            logger.error("Extracted Parameters:");
            logger.error("Mobile Number: {}", mobileNumber);
            logger.error("Plan ID: {}", planId);
            logger.error("Amount: {}", amount);
            logger.error("Email: {}", email);

            // Create Razorpay order
            Map<String, Object> orderResponse = razorpayService.createRazorpayOrder(
                    mobileNumber, planId, amount, email
            );

            // Log successful order creation
            logger.info("Razorpay Order Created Successfully: {}", orderResponse);

            return ResponseEntity.ok(orderResponse);
        } catch (Exception e) {
            // Log the full exception
            logger.error("Unexpected error in createRazorpayOrder", e);

            // Return a generic error response
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Order creation failed",
                    "details", e.getMessage()
            ));
        }
    }

    // Helper method to safely extract string parameter
    private String extractStringParam(Map<String, Object> request, String key, String defaultValue) {
        Object value = request.get(key);
        if (value == null) {
            logger.warn("Parameter {} is null, using default: {}", key, defaultValue);
            return defaultValue;
        }
        return value.toString().trim();
    }

    // Helper method to safely extract integer parameter
    private Integer extractIntegerParam(Map<String, Object> request, String key, Integer defaultValue) {
        Object value = request.get(key);
        if (value == null) {
            logger.warn("Parameter {} is null, using default: {}", key, defaultValue);
            return defaultValue;
        }

        try {
            // Handle different input types
            if (value instanceof Integer) {
                return (Integer) value;
            }
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            logger.error("Failed to parse {} as integer: {}", key, value);
            return defaultValue;
        }
    }

    // Helper method to safely extract BigDecimal parameter
    private BigDecimal extractBigDecimalParam(Map<String, Object> request, String key, BigDecimal defaultValue) {
        Object value = request.get(key);
        if (value == null) {
            logger.warn("Parameter {} is null, using default: {}", key, defaultValue);
            return defaultValue;
        }

        try {
            // Handle different input types
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            }
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            logger.error("Failed to parse {} as BigDecimal: {}", key, value);
            return defaultValue;
        }
    }
}