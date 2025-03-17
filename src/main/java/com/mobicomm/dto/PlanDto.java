package com.mobicomm.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@Data
public class PlanDto {
    @JsonProperty("id")
    private Integer planId;

    private Integer categoryId;

    @JsonProperty("category")
    private String categoryName;

    private String planName;
    private String description;
    private BigDecimal price;
    private String data;
    private String validity;
    private String calls;
    private String benefits;

    @JsonProperty("active")
    private Boolean isActive;
}