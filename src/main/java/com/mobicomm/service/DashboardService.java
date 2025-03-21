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
import java.math.BigDecimal;

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
        LocalDateTime oneDayFromNow = now.plusDays(1);

        List<UserExpiringPlanDto> result = new ArrayList<>();

        // Modify the method call to match the repository method signature
        List<User> usersWithExpiringPlans = userRepository.findUsersWithPlansExpiringInThreeToOneDays(now, threeDaysFromNow, oneDayFromNow);

        for (User user : usersWithExpiringPlans) {
            UserExpiringPlanDto dto = new UserExpiringPlanDto();
            dto.setUserId(user.getUserId());
            dto.setMobileNumber(user.getMobileNumber());

            // Calculate expiry date and days remaining
            if (user.getLastRechargeDate() != null) {
                LocalDateTime lastRechargeDate = user.getLastRechargeDate().toLocalDateTime();
                LocalDateTime expiryDate = lastRechargeDate.plusDays(30); // Assuming 30-day validity
                dto.setExpiryDate(expiryDate);
            }

            // Find the most recent transaction to get accurate plan details
            Optional<Transaction> latestTransaction = transactionRepository
                    .findByUserIdOrderByTransactionDateDesc(user.getUserId())
                    .stream()
                    .findFirst();

            if (latestTransaction.isPresent()) {
                Transaction transaction = latestTransaction.get();

                // Set payment status
                dto.setPaymentStatus(transaction.getPaymentStatus());

                // Get plan details
                if (transaction.getPlan() != null) {
                    // Set plan name
                    dto.setPlanName(transaction.getPlan().getDataLimit() + " Plan");

                    // Set plan price correctly
                    BigDecimal planPrice = transaction.getPlan().getPrice();
                    dto.setAmount(planPrice != null ? planPrice : BigDecimal.ZERO);
                } else {
                    dto.setPlanName("Uncategorized");
                    dto.setAmount(BigDecimal.ZERO);
                }
            } else {
                dto.setPlanName("Uncategorized");
                dto.setAmount(BigDecimal.ZERO);
            }

            result.add(dto);
        }

        return result;
    }

}