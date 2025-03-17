package com.mobicomm.controller;

import com.mobicomm.dto.StripeCheckoutRequest;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stripe")
@CrossOrigin(origins = "*")
public class StripeController {
    private static final Logger logger = LoggerFactory.getLogger(StripeController.class);

    @Value("${stripe.api.key}")
    private String stripeSecretKey;

    // In your current code you seem to have a separate publishable key
    @Value("${stripe.publishable.key:pk_test_yourPublishableKey}")
    private String stripePublishableKey;

    @PostMapping("/create-checkout-session")
    public ResponseEntity<?> createCheckoutSession(@RequestBody StripeCheckoutRequest request) {
        try {
            // Log the received request for debugging
            logger.info("Received checkout request: {}", request);

            // Initialize Stripe (if not using a config class)
            Stripe.apiKey = stripeSecretKey;

            // Create session
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("http://localhost:63342/success.html")
                    .setCancelUrl("http://localhost:63342/cancel.html")
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("inr")
                                                    .setUnitAmount(request.getPrice().multiply(new BigDecimal(100)).longValue())
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(request.getPlanName())
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .putMetadata("mobileNumber", request.getMobileNumber())
                    .putMetadata("planId", request.getPlanId().toString())
                    .build();

            Session session = Session.create(params);

            // Return session information
            Map<String, String> response = new HashMap<>();
            response.put("sessionId", session.getId());
            response.put("publicKey", stripePublishableKey);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error creating checkout session", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}