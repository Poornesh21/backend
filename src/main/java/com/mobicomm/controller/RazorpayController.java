package com.mobicomm.controller;

import com.mobicomm.dto.RechargeRequest;
import com.mobicomm.dto.TransactionDto;
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
        // Log that we received a request to this endpoint
        logger.info("Received create-order request with path /api/razorpay/create-order");
        try {
            // Log entire request for debugging
            logger.info("Creating order with request: {}", request);

            // Extract parameters with safe defaults and detailed logging
            String mobileNumber = extractStringParam(request, "mobileNumber", "");
            Integer planId = extractIntegerParam(request, "planId", 1);
            BigDecimal amount = extractBigDecimalParam(request, "amount", BigDecimal.ZERO);
            String email = extractStringParam(request, "email", "");

            // Detailed logging of extracted parameters
            logger.info("Extracted Parameters:");
            logger.info("Mobile Number: {}", mobileNumber);
            logger.info("Plan ID: {}", planId);
            logger.info("Amount: {}", amount);
            logger.info("Email: {}", email);

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

    /**
     * Verify Razorpay payment and process transaction
     */
    @PostMapping("/verify-payment")
    public ResponseEntity<Map<String, Object>> verifyPayment(@RequestBody Map<String, Object> request) {
        try {
            logger.info("Verifying payment with request: {}", request);

            // Extract verification parameters
            String razorpayOrderId = extractStringParam(request, "razorpay_order_id", "");
            String razorpayPaymentId = extractStringParam(request, "razorpay_payment_id", "");
            String razorpaySignature = extractStringParam(request, "razorpay_signature", "");

            // Extract transaction details
            String mobileNumber = extractStringParam(request, "mobileNumber", "");
            Integer planId = extractIntegerParam(request, "planId", 1);
            BigDecimal amount = extractBigDecimalParam(request, "amount", BigDecimal.ZERO);
            String email = extractStringParam(request, "email", "");

            // Verify payment signature
            boolean isValidSignature = razorpayService.verifyRazorpaySignature(
                    razorpayOrderId, razorpayPaymentId, razorpaySignature);

            // For test/development environment, continue even with invalid signature
            boolean isTestEnvironment = razorpayKeyId != null && razorpayKeyId.startsWith("rzp_test_");

            if (!isValidSignature && !isTestEnvironment) {
                logger.error("Invalid Razorpay signature");
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Invalid payment signature"
                ));
            }

            // Log that we're accepting this payment (either valid signature or test environment)
            logger.info("Payment accepted: {}", isValidSignature ? "Valid signature" : "Test environment override");

            // Create recharge request for processing
            RechargeRequest rechargeRequest = new RechargeRequest();
            rechargeRequest.setMobileNumber(mobileNumber);
            rechargeRequest.setPlanId(planId);
            rechargeRequest.setAmount(amount);
            rechargeRequest.setPaymentMethod("Razorpay | " + razorpayPaymentId);
            rechargeRequest.setPaymentStatus("Completed");
            rechargeRequest.setTransactionDate(LocalDateTime.now());

            // Calculate expiry date based on plan validity (typical 30 days)
            LocalDateTime expiryDate = LocalDateTime.now().plusDays(30);
            rechargeRequest.setExpiryDate(expiryDate);

            // Add email for notifications
            rechargeRequest.setEmail(email);

            // Process the recharge
            TransactionDto transactionDto = transactionService.processRecharge(rechargeRequest);
            logger.info("Transaction processed successfully: {}", transactionDto);

            // Return success response
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Payment verified and transaction processed",
                    "transactionId", transactionDto.getTransactionId(),
                    "razorpayPaymentId", razorpayPaymentId
            ));

        } catch (Exception e) {
            logger.error("Error verifying payment", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Payment verification failed: " + e.getMessage()
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
        if (value == null || "undefined".equals(value.toString())) {
            logger.info("Parameter {} is null or undefined, using default: {}", key, defaultValue);
            return defaultValue;
        }

        try {
            // Handle different input types
            if (value instanceof Integer) {
                return (Integer) value;
            }
            // Handle string values
            String stringValue = value.toString().trim();
            if (stringValue.isEmpty()) {
                logger.info("Parameter {} is empty string, using default: {}", key, defaultValue);
                return defaultValue;
            }
            return Integer.parseInt(stringValue);
        } catch (NumberFormatException e) {
            logger.info("Failed to parse {} as integer: {}, using default: {}", key, value, defaultValue);
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