package com.mobicomm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "recharge_plans")
@Data                // Generates getters, setters, equals, hashCode, and toString methods
@NoArgsConstructor   // Generates no-args constructor
@AllArgsConstructor  // Generates constructor with all args
public class Plan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private Integer planId;

    @Column(name = "category_id", nullable = false)
    private Integer categoryId;


    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "validity_days", nullable = false)
    private Integer validityDays;

    @Column(name = "data_limit", length = 50)
    private String dataLimit;

    @Column(name = "ott_benefits")
    private String ottBenefits;

    @Column(name = "status", nullable = false, columnDefinition = "enum('Active','Inactive') default 'Active'")
    private String status;


    // Custom methods need to be retained as Lombok doesn't generate these
    // Helper methods for compatibility with existing code
    public Boolean getIsActive() {
        return "Active".equals(this.status);
    }

    public void setIsActive(Boolean isActive) {
        this.status = isActive ? "Active" : "Inactive";
    }
}