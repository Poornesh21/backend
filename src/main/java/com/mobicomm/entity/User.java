package com.mobicomm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;
    
    @Column(name = "mobile_number", nullable = false, unique = true)
    private String mobileNumber;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "last_recharge_date")
    private Timestamp lastRechargeDate;
    
    @Column(name = "role_id")
    private Integer roleId;
}