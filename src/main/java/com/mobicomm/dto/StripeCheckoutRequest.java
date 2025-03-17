package com.mobicomm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class StripeCheckoutRequest {
    @NotBlank(message = "Mobile number is required")
    private String mobileNumber;

    @NotNull(message = "Plan ID is required")
    private Integer planId;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;

    @NotBlank(message = "Plan name is required")
    private String planName;

    // Constructors
    public StripeCheckoutRequest() {}

    public StripeCheckoutRequest(String mobileNumber, Integer planId, BigDecimal price, String planName) {
        this.mobileNumber = mobileNumber;
        this.planId = planId;
        this.price = price;
        this.planName = planName;
    }

    // Getters and Setters
    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public Integer getPlanId() {
        return planId;
    }

    public void setPlanId(Integer planId) {
        this.planId = planId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }
}