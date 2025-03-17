package com.mobicomm.repository;

import com.mobicomm.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    /**
     * Find transactions that will expire soon.
     * Looks for transactions expiring within the next 7 days
     * and are in 'Completed' status.
     */
    @Query("SELECT t FROM Transaction t " +
            "WHERE t.expiryDate BETWEEN :now AND :sevenDaysFromNow " +
            "AND t.paymentStatus = 'Completed'")
    List<Transaction> findSoonToExpireTransactions(
            @Param("now") LocalDateTime now,
            @Param("sevenDaysFromNow") LocalDateTime sevenDaysFromNow
    );

    /**
     * Find transactions for a given user ordered by transactionDate descending.
     */
    List<Transaction> findByUserIdOrderByTransactionDateDesc(Integer userId);

    /**
     * Find transactions for a given user with expiry date between the given dates.
     * This is specifically added for the dashboard to find users with expiring plans.
     */
    List<Transaction> findByUserIdAndExpiryDateBetween(
            Integer userId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * Find transactions whose expiryDate is between the given start and end dates.
     */
    List<Transaction> findByExpiryDateBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find a transaction by its payment reference.
     */
    Optional<Transaction> findByPaymentMethod(String paymentMethod);

    /**
     * Find a transaction by its reference (stored in paymentMethod field).
     */
    default Optional<Transaction> findByPaymentReference(String reference) {
        return findByPaymentMethod(reference);
    }

    /**
     * Find transactions whose transactionDate is between the given start and end dates.
     */
    List<Transaction> findByTransactionDateBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find transactions with transactionDate after the specified date, ordered descending.
     */
    List<Transaction> findByTransactionDateAfterOrderByTransactionDateDesc(LocalDateTime date);

    /**
     * Find transactions with optional filtering by status, transaction date range, and plan.
     */
    @Query("SELECT t FROM Transaction t " +
            "WHERE (:status IS NULL OR t.paymentStatus = :status) " +
            "AND (:fromDateTime IS NULL OR t.transactionDate >= :fromDateTime) " +
            "AND (:toDateTime IS NULL OR t.transactionDate <= :toDateTime) " +
            "AND (:planId IS NULL OR t.plan.planId = :planId)")
    List<Transaction> findWithFilters(@Param("status") String status,
                                      @Param("fromDateTime") LocalDateTime fromDateTime,
                                      @Param("toDateTime") LocalDateTime toDateTime,
                                      @Param("planId") Integer planId);

    /**
     * Find transactions that are expiring within a specific date range.
     */
    @Query("SELECT t FROM Transaction t WHERE t.expiryDate BETWEEN :startDate AND :endDate")
    List<Transaction> findExpiringTransactions(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    /**
     * Find transactions for users whose plans are expiring within a specific date range.
     * Groups by user ID to get unique users.
     */
    @Query("SELECT t FROM Transaction t " +
            "WHERE t.expiryDate BETWEEN :startDate AND :endDate " +
            "AND t.paymentStatus IN ('Completed', 'Success') " +
            "GROUP BY t.userId")
    List<Transaction> findUsersWithExpiringPlans(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}