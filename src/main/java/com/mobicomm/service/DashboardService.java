package com.mobicomm.service;

import com.mobicomm.dto.TransactionDto;
import com.mobicomm.dto.UserExpiringPlanDto;
import com.mobicomm.entity.PlanCategory;
import com.mobicomm.entity.Transaction;
import com.mobicomm.entity.User;
import com.mobicomm.repository.TransactionRepository;
import com.mobicomm.repository.UserRepository;
import com.mobicomm.repository.PlanCategoryRepository;
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

    @Autowired
    private PlanCategoryRepository planCategoryRepository;

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

        // Query to find users with plans expiring based on last_recharge_date
        List<User> usersWithExpiringPlans = userRepository.findUsersWithPlansExpiringInOneToThreeDays(now, threeDaysFromNow);

        List<UserExpiringPlanDto> result = new ArrayList<>();

        for (User user : usersWithExpiringPlans) {
            // Find the most recent transaction for this user
            Optional<Transaction> latestTransaction = transactionRepository
                    .findByUserIdOrderByTransactionDateDesc(user.getUserId())
                    .stream()
                    .findFirst();

            if (latestTransaction.isPresent()) {
                Transaction transaction = latestTransaction.get();

                UserExpiringPlanDto dto = new UserExpiringPlanDto();
                dto.setUserId(user.getUserId());
                dto.setMobileNumber(user.getMobileNumber());
                dto.setExpiryDate(transaction.getExpiryDate());
                dto.setPaymentStatus(transaction.getPaymentStatus());

                // Get the category name from the plan
                try {
                    if (transaction.getPlan() != null && transaction.getPlan().getCategoryId() != null) {
                        Integer categoryId = transaction.getPlan().getCategoryId();
                        Optional<PlanCategory> category = planCategoryRepository.findById(categoryId);

                        dto.setPlanName(category.map(PlanCategory::getCategoryName)
                                .orElse("Uncategorized"));
                    } else {
                        dto.setPlanName("Uncategorized");
                    }
                } catch (Exception e) {
                    logger.error("Error retrieving category for transaction: {}", transaction.getTransactionId(), e);
                    dto.setPlanName("Uncategorized");
                }

                result.add(dto);
            }
        }

        return result;
    }
}