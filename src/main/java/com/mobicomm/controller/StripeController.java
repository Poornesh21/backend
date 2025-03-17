package com.mobicomm.controller;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*") // Enable CORS for all origins during development
public class StripeController {

    public StripeController() {
        // Initialize with your secret key
        Stripe.apiKey = "sk_test_51R3WxgRDO3h57yCpyi3sJXJ3amNgfuyuPRLULxSOj0pyd9RlIQizGqTJGoaVIXqOznlUf5oTGYZb0TNjgnlzRFnU00OIWssyE6";
    }

    @PostMapping("/create-checkout")
    public Map<String, String> checkout(@RequestBody Map<String, Object> request) {
        try {
            System.out.println("Received payment request: " + request); // Log incoming request

            // Get price from request
            double price = Double.parseDouble(request.get("price").toString());

            // Create session
            SessionCreateParams params = SessionCreateParams.builder()
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("inr")
                                                    .setUnitAmount((long)(price * 100))
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("MobiComm Recharge")
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .setQuantity(1L)
                                    .build()
                    )
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("http://localhost:63342/success.html")
                    .setCancelUrl("http://localhost:63342/cancel.html")
                    .build();

            Session session = Session.create(params);

            // Return URL for redirect
            Map<String, String> response = new HashMap<>();
            response.put("url", session.getUrl());
            System.out.println("Created Stripe checkout URL: " + session.getUrl()); // Log the URL
            return response;

        } catch (Exception e) {
            System.err.println("Error creating checkout session: " + e.getMessage()); // Log any errors
            e.printStackTrace();

            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }

    // Add a test endpoint to verify server is accessible
    @GetMapping("/ping")
    public Map<String, String> ping() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "Server is running");
        return response;
    }
}