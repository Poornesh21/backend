package com.mobicomm.repository;

import com.mobicomm.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    /**
     * Find a user by their mobile number
     */
    Optional<User> findByMobileNumber(String mobileNumber);

    /**
     * Find users with transactions expiring within a given date range
     * This query joins the users and transactions tables to find users
     * whose plans are expiring soon
     */
    @Query("""
        SELECT DISTINCT u FROM User u 
        JOIN Transaction t ON u.userId = t.userId 
        WHERE t.expiryDate BETWEEN :startDate AND :endDate 
        AND t.paymentStatus IN ('Completed', 'Success')
    """)
    List<User> findUsersWithExpiringPlans(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find users with last recharge date between the specified dates
     * This is useful for finding users who recently recharged
     */
    @Query("SELECT u FROM User u WHERE u.lastRechargeDate BETWEEN :startDate AND :endDate")
    List<User> findByLastRechargeDateBetween(
            @Param("startDate") java.sql.Timestamp startDate,
            @Param("endDate") java.sql.Timestamp endDate
    );

    /**
     * Find users with plans expiring within the next 1-3 days based on transaction data
     * This more specific query helps with the dashboard functionality
     */
    @Query("""
        SELECT DISTINCT u FROM User u 
        JOIN Transaction t ON u.userId = t.userId 
        WHERE t.expiryDate BETWEEN :startDate AND :endDate 
        AND t.paymentStatus IN ('Completed', 'Success') 
        ORDER BY t.expiryDate ASC
    """)
    List<User> findUsersWithPlansExpiringInOneToThreeDays(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}