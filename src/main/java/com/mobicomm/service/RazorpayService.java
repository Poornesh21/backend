package com.mobicomm.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class RazorpayService {

    private static final Logger logger = LoggerFactory.getLogger(RazorpayService.class);

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Autowired
    private TransactionService transactionService;

    /**
     * Create a Razorpay order for a mobile recharge
     */
    public Map<String, Object> createRazorpayOrder(
            String mobileNumber,
            Integer planId,
            BigDecimal amount,
            String email
    ) {
        try {
            // Initialize Razorpay client
            RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            // Convert amount to paise (Razorpay requires amount in smallest currency unit)
            int amountInPaise = amount.multiply(BigDecimal.valueOf(100)).intValue();

            // Create order options
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "MobiComm_" + System.currentTimeMillis());

            // Add notes for tracking
            JSONObject notes = new JSONObject();
            notes.put("mobile_number", mobileNumber);
            notes.put("plan_id", planId);
            notes.put("email", email);
            orderRequest.put("notes", notes);

            // Create order
            Order order = razorpayClient.orders.create(orderRequest);

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("razorpay_key_id", razorpayKeyId);
            response.put("order_id", order.get("id"));
            response.put("amount", amountInPaise);
            response.put("currency", "INR");
            response.put("mobile_number", mobileNumber);
            response.put("plan_id", planId);
            response.put("email", email);

            return response;
        } catch (RazorpayException e) {
            logger.error("Error creating Razorpay order", e);
            throw new RuntimeException("Failed to create Razorpay order", e);
        }
    }

    /**
     * Verify Razorpay payment signature
     */
    public boolean verifyRazorpaySignature(
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature
    ) {
        try {
            // Construct the signature verification payload
            String payload = razorpayOrderId + "|" + razorpayPaymentId;

            // Use HMAC SHA256 signature verification
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);

            byte[] hash = sha256_HMAC.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = Base64.getEncoder().encodeToString(hash);

            // Compare calculated signature with received signature
            return calculatedSignature.equals(razorpaySignature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Error verifying Razorpay signature", e);
            return false;
        }
    }
}