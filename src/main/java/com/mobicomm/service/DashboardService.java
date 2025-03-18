package com.mobicomm.service;

import com.mobicomm.dto.TransactionDto;
import com.mobicomm.dto.UserExpiringPlanDto;
import com.mobicomm.entity.Transaction;
import com.mobicomm.entity.User;
import com.mobicomm.repository.TransactionRepository;
import com.mobicomm.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionService transactionService;

    /**
     * Get user transaction history.
     */
    @Transactional(readOnly = true)
    public List<TransactionDto> getUserTransactionHistory(Integer userId) {
        logger.info("Fetching transaction history for user ID: {}", userId);

        // Verify user exists
        if (!userRepository.existsById(userId)) {
            logger.error("User not found with ID: {}", userId);
            throw new RuntimeException("User not found with ID: " + userId);
        }


        // Find transactions for the user
        List<Transaction> userTransactions = transactionRepository.findByUserIdOrderByTransactionDateDesc(userId);
        logger.info("Found {} transactions for user", userTransactions.size());

        return userTransactions.stream()
                .map(transactionService::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get users with plans expiring soon (1-3 days).
     * This method focuses specifically on the transaction data for expiring plans.
     */
    @Transactional(readOnly = true)
    public List<UserExpiringPlanDto> getUsersWithExpiringPlans() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeDaysFromNow = now.plusDays(3);

        logger.info("Fetching users with plans expiring in 1-3 days");
        logger.info("Current Date: {}", now);
        logger.info("Three Days Cutoff: {}", threeDaysFromNow);

        // Get all transactions expiring soon
        List<Transaction> expiringTransactions = transactionRepository.findByExpiryDateBetween(now, threeDaysFromNow);
        logger.info("Found {} transactions expiring within 3 days", expiringTransactions.size());

        List<UserExpiringPlanDto> result = new ArrayList<>();

        // For each transaction, get the user and create a DTO
        for (Transaction transaction : expiringTransactions) {
            try {
                Optional<User> userOpt = userRepository.findById(transaction.getUserId());
                if (userOpt.isPresent()) {
                    User user = userOpt.get();

                    UserExpiringPlanDto dto = new UserExpiringPlanDto();
                    dto.setUserId(user.getUserId());
                    dto.setMobileNumber(user.getMobileNumber());

                    // Key change: Always use transaction's expiry date
                    dto.setExpiryDate(transaction.getExpiryDate());
                    dto.setPaymentStatus(transaction.getPaymentStatus());

                    // Set plan name based on transaction's plan
                    dto.setPlanName(transaction.getPlan() != null
                            ? transaction.getPlan().getDataLimit() + " Plan"
                            : "Unnamed Plan");

                    result.add(dto);
                }
            } catch (Exception e) {
                logger.error("Error processing transaction with ID {}: {}",
                        transaction.getTransactionId(), e.getMessage());
            }
        }

        return result;
    }
}