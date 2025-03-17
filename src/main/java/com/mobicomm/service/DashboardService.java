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
     * Get all upcoming expiring transactions (between now and 30 days from now).
     */
    @Transactional(readOnly = true)
    public List<TransactionDto> getAllUpcomingExpiringTransactions() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureDate = now.plusDays(30);

        logger.info("Fetching upcoming expiring transactions");
        logger.info("Current Date: {}", now);
        logger.info("Future Date Cutoff: {}", futureDate);

        // Find expiring transactions
        List<Transaction> upcomingExpiringTransactions = transactionRepository.findByExpiryDateBetween(now, futureDate);
        logger.info("Found {} upcoming expiring transactions", upcomingExpiringTransactions.size());

        return upcomingExpiringTransactions.stream()
                .map(transaction -> {
                    TransactionDto dto = transactionService.convertToDto(transaction);
                    // Fetch user details
                    try {
                        Optional<User> user = userRepository.findById(transaction.getUserId());
                        user.ifPresent(u -> {
                            dto.setMobileNumber(u.getMobileNumber());
                            dto.setUserId(u.getUserId());
                        });
                    } catch (Exception e) {
                        logger.error("Error fetching user details for transaction", e);
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Fetch transactions expiring within the next 3 days.
     */
    @Transactional(readOnly = true)
    public List<TransactionDto> getExpiringTransactionsWithin3Days() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeDaysFromNow = now.plusDays(3);

        logger.info("Searching for transactions expiring within 3 days");
        logger.info("Current Time: {}", now);
        logger.info("Three Days From Now: {}", threeDaysFromNow);

        // Find expiring transactions using one repository method
        List<Transaction> expiringTransactions = transactionRepository.findByExpiryDateBetween(now, threeDaysFromNow);
        logger.info("Found {} transactions expiring within 3 days", expiringTransactions.size());

        return expiringTransactions.stream()
                .map(transaction -> {
                    TransactionDto dto = transactionService.convertToDto(transaction);
                    // Fetch user details
                    try {
                        Optional<User> user = userRepository.findById(transaction.getUserId());
                        user.ifPresent(u -> {
                            dto.setMobileNumber(u.getMobileNumber());
                            dto.setUserId(u.getUserId());
                        });
                    } catch (Exception e) {
                        logger.error("Error fetching user details for transaction", e);
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get users with plans expiring soon (1-3 days).
     * This method focuses specifically on the user data along with their expiring plans.
     */
    @Transactional(readOnly = true)
    public List<UserExpiringPlanDto> getUsersWithExpiringPlans() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeDaysFromNow = now.plusDays(3);

        logger.info("Fetching users with plans expiring in 1-3 days");
        logger.info("Current Date: {}", now);
        logger.info("Three Days Cutoff: {}", threeDaysFromNow);

        // Simplified approach to avoid LazyInitializationException:
        // First get all transactions expiring soon
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
                    dto.setEmail(user.getEmail());
                    dto.setExpiryDate(transaction.getExpiryDate());
                    dto.setPaymentStatus(transaction.getPaymentStatus());


                    result.add(dto);
                }
            } catch (Exception e) {
                logger.error("Error processing transaction with ID {}: {}",
                        transaction.getTransactionId(), e.getMessage());
            }
        }

        return result;
    }

    /**
     * Get transactions expiring soon (within 7 days).
     */
    @Transactional(readOnly = true)
    public List<TransactionDto> getSoonToExpireTransactions() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysFromNow = now.plusDays(7);

        logger.info("Searching for soon to expire transactions");
        logger.info("Current Time: {}", now);
        logger.info("Seven Days From Now: {}", sevenDaysFromNow);

        // Find transactions expiring soon using a different repository method
        List<Transaction> soonToExpireTransactions = transactionRepository.findSoonToExpireTransactions(now, sevenDaysFromNow);
        logger.info("Found {} transactions expiring soon", soonToExpireTransactions.size());

        return soonToExpireTransactions.stream()
                .map(transaction -> {
                    TransactionDto dto = transactionService.convertToDto(transaction);
                    // Fetch user details
                    try {
                        Optional<User> user = userRepository.findById(transaction.getUserId());
                        user.ifPresent(u -> {
                            dto.setMobileNumber(u.getMobileNumber());
                            dto.setUserId(u.getUserId());
                        });
                    } catch (Exception e) {
                        logger.error("Error fetching user details for transaction", e);
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }
}