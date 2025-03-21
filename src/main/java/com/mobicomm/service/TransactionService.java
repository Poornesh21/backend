package com.mobicomm.service;

import com.mobicomm.dto.RechargeRequest;
import com.mobicomm.dto.TransactionDto;
import com.mobicomm.entity.Plan;
import com.mobicomm.entity.Transaction;
import com.mobicomm.entity.User;
import com.mobicomm.exception.ResourceNotFoundException;
import com.mobicomm.repository.PlanRepository;
import com.mobicomm.repository.TransactionRepository;
import com.mobicomm.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TransactionService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private EmailService emailService;

    /**
     * Process a recharge request and create transaction
     * Updated to handle Razorpay payments and send email notifications
     */
    @Transactional
    public TransactionDto processRecharge(RechargeRequest request) {
        logger.debug("Processing recharge request: {}", request);

        // Validate planId is not null
        if (request.getPlanId() == null) {
            logger.error("Plan ID cannot be null");
            throw new IllegalArgumentException("Plan ID cannot be null");
        }

        // Find or create user for this mobile number
        User user = findOrCreateUser(request.getMobileNumber());

        // Update user email if provided in the request
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            logger.debug("Updating user email to: {}", request.getEmail());
            user.setEmail(request.getEmail());
            userRepository.save(user);
        }

        // Validate plan exists
        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan", request.getPlanId()));
        logger.debug("Found plan: {}", plan.getPlanId());

        // Create transaction entity
        Transaction transaction = new Transaction();
        transaction.setUserId(user.getUserId());
        transaction.setPlan(plan);
        transaction.setAmount(request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO);

        // Handle payment status (default to Completed if not specified)
        String paymentStatus = request.getPaymentStatus();
        if (paymentStatus == null || paymentStatus.isEmpty()) {
            paymentStatus = "Completed";
        }
        transaction.setPaymentStatus(paymentStatus);

        // Save payment method (including Razorpay ID if available)
        transaction.setPaymentMethod(request.getPaymentMethod());

        // Set transaction date (default to now if not specified)
        LocalDateTime transactionDate = request.getTransactionDate() != null ?
                request.getTransactionDate() : LocalDateTime.now();
        transaction.setTransactionDate(transactionDate);

        // Set expiry date based on plan validity or default
        LocalDateTime expiryDate;
        if (request.getExpiryDate() != null) {
            expiryDate = request.getExpiryDate();
        } else if (plan.getValidityDays() != null && plan.getValidityDays() > 0) {
            expiryDate = transactionDate.plusDays(plan.getValidityDays());
        } else {
            expiryDate = transactionDate.plusDays(30);
        }
        transaction.setExpiryDate(expiryDate);

        // IMPORTANT: Update user's last recharge date
        // Convert LocalDateTime to Timestamp for database compatibility
        java.sql.Timestamp lastRechargeTimestamp = java.sql.Timestamp.valueOf(transactionDate);
        user.setLastRechargeDate(lastRechargeTimestamp);
        userRepository.save(user);
        logger.debug("Updated user's last recharge date to: {}", lastRechargeTimestamp);

        // Save transaction
        Transaction savedTransaction = transactionRepository.save(transaction);
        logger.info("Transaction saved successfully with ID: {}", savedTransaction.getTransactionId());

        // Convert to DTO
        TransactionDto transactionDto = convertToDto(savedTransaction);

        // Send email notifications if transaction is successful and email is available
        if (("Completed".equalsIgnoreCase(paymentStatus) || "Success".equalsIgnoreCase(paymentStatus))
                && user.getEmail() != null && !user.getEmail().isEmpty()) {

            try {
                logger.info("Attempting to send invoice email to: {}", user.getEmail());

                // Get plan name for email
                String planName = plan.getDataLimit() != null ?
                        plan.getDataLimit() + " GB Plan" : "Mobile Recharge Plan";

                // Format amount for email
                String amountStr = transaction.getAmount().toString();

                // Send invoice email
                boolean emailSent = emailService.sendInvoiceEmail(
                        user.getEmail(),
                        user.getMobileNumber(),
                        planName,
                        amountStr,
                        savedTransaction.getTransactionId().toString(),
                        savedTransaction.getPaymentMethod(),
                        savedTransaction.getTransactionDate().toString()
                );

                if (emailSent) {
                    logger.info("Invoice email sent successfully to: {}", user.getEmail());
                } else {
                    logger.warn("Failed to send invoice email to: {}", user.getEmail());
                }

            } catch (Exception e) {
                // Log the error but don't fail the transaction
                logger.error("Error sending email notification: {}", e.getMessage(), e);
            }
        } else {
            logger.info("Email notification not sent. Status: {}, Email: {}",
                    paymentStatus, user.getEmail());
        }

        return transactionDto;
    }

    /**
     * Find existing user or create a new user record if not found
     */
    private User findOrCreateUser(String mobileNumber) {
        logger.debug("Looking up user for mobile number: {}", mobileNumber);
        Optional<User> userOpt = userRepository.findByMobileNumber(mobileNumber);

        if (userOpt.isPresent()) {
            logger.debug("Found existing user with ID: {}", userOpt.get().getUserId());
            return userOpt.get();
        } else {
            logger.info("User not found with mobile number: {}. Creating new user", mobileNumber);
            // Create a new user for this mobile number
            User newUser = new User();
            newUser.setMobileNumber(mobileNumber);
            // Set default role for new users (2 = regular user)
            newUser.setRoleId(2);

            User savedUser = userRepository.save(newUser);
            logger.info("Created new user with ID: {}", savedUser.getUserId());
            return savedUser;
        }
    }

    /**
     * Get transactions by mobile number
     */
    public List<TransactionDto> getTransactionsByMobileNumber(String mobileNumber) {
        logger.debug("Getting transactions for mobile number: {}", mobileNumber);
        Optional<User> user = userRepository.findByMobileNumber(mobileNumber);

        if (user.isPresent()) {
            List<Transaction> transactions = transactionRepository.findByUserIdOrderByTransactionDateDesc(user.get().getUserId());
            logger.debug("Found {} transactions for mobile: {}", transactions.size(), mobileNumber);
            return transactions.stream().map(this::convertToDto).collect(Collectors.toList());
        }

        logger.warn("No user found with mobile number: {}", mobileNumber);
        return Collections.emptyList();
    }

    /**
     * Get transactions by user ID
     */
    public List<TransactionDto> getTransactionsByUserId(Integer userId) {
        logger.debug("Getting transactions for user ID: {}", userId);
        List<Transaction> transactions = transactionRepository.findByUserIdOrderByTransactionDateDesc(userId);
        logger.debug("Found {} transactions for user ID: {}", transactions.size(), userId);
        return transactions.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    /**
     * Get transaction by ID
     */
    public TransactionDto getTransactionById(Integer id) {
        logger.debug("Getting transaction with ID: {}", id);
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
        return convertToDto(transaction);
    }

    /**
     * Get transaction by payment reference
     */
    public TransactionDto getTransactionByReference(String reference) {
        logger.debug("Looking up transaction by reference: {}", reference);
        Optional<Transaction> transaction = transactionRepository.findByPaymentReference(reference);
        if (transaction.isPresent()) {
            logger.debug("Found transaction: {}", transaction.get().getTransactionId());
        } else {
            logger.debug("No transaction found with reference: {}", reference);
        }
        return transaction.map(this::convertToDto).orElse(null);
    }

    /**
     * Get all transactions with optional filtering
     */
    public List<TransactionDto> getAllTransactionsWithFilters(String status, LocalDate fromDate, LocalDate toDate, Integer planId) {
        logger.debug("Getting transactions with filters - status: {}, fromDate: {}, toDate: {}, planId: {}",
                status, fromDate, toDate, planId);

        List<Transaction> transactions;

        if (status != null || fromDate != null || toDate != null || planId != null) {
            // Convert dates to LocalDateTime for comparison
            LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
            LocalDateTime toDateTime = toDate != null ? toDate.atTime(LocalTime.MAX) : null;

            transactions = transactionRepository.findWithFilters(status, fromDateTime, toDateTime, planId);
        } else {
            transactions = transactionRepository.findAll();
        }

        logger.debug("Found {} transactions matching filters", transactions.size());
        return transactions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get transaction statistics
     */
    public Map<String, Object> getTransactionStatistics(LocalDate fromDate, LocalDate toDate) {
        logger.debug("Generating transaction statistics from: {} to: {}", fromDate, toDate);
        Map<String, Object> statistics = new HashMap<>();

        // Set default date range if not provided (last 30 days)
        LocalDateTime fromDateTime = fromDate != null ?
                fromDate.atStartOfDay() :
                LocalDateTime.now().minusDays(30);

        LocalDateTime toDateTime = toDate != null ?
                toDate.atTime(LocalTime.MAX) :
                LocalDateTime.now();

        // Get all transactions in the date range
        List<Transaction> transactions = transactionRepository.findByTransactionDateBetween(fromDateTime, toDateTime);
        logger.debug("Found {} transactions in date range", transactions.size());

        // Calculate statistics
        BigDecimal totalRevenue = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long successfulTransactions = transactions.stream()
                .filter(t -> "SUCCESS".equalsIgnoreCase(t.getPaymentStatus()) || "COMPLETED".equalsIgnoreCase(t.getPaymentStatus()))
                .count();

        long failedTransactions = transactions.size() - successfulTransactions;

        // Get the most popular plan
        Map<Integer, Long> planCounts = new HashMap<>();

        for (Transaction transaction : transactions) {
            Integer planId = transaction.getPlan().getPlanId();
            planCounts.put(planId, planCounts.getOrDefault(planId, 0L) + 1);
        }

        Integer mostPopularPlanId = null;
        long maxCount = 0;

        for (Map.Entry<Integer, Long> entry : planCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostPopularPlanId = entry.getKey();
            }
        }

        // Put all statistics in the map
        statistics.put("totalTransactions", transactions.size());
        statistics.put("totalRevenue", totalRevenue);
        statistics.put("successfulTransactions", successfulTransactions);
        statistics.put("failedTransactions", failedTransactions);
        statistics.put("startDate", fromDateTime);
        statistics.put("endDate", toDateTime);

        // Add most popular plan info if available
        if (mostPopularPlanId != null) {
            Optional<Plan> popularPlan = planRepository.findById(mostPopularPlanId);
            if (popularPlan.isPresent()) {
                statistics.put("popularPlan", popularPlan.get().getDataLimit() + " Plan");
            }
        }

        logger.debug("Generated statistics: {}", statistics);
        return statistics;
    }

    /**
     * Get recent transactions (last X days)
     */
    public List<TransactionDto> getRecentTransactions(Integer days) {
        logger.debug("Getting transactions from the last {} days", days);
        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
        List<Transaction> transactions = transactionRepository.findByTransactionDateAfterOrderByTransactionDateDesc(fromDate);
        logger.debug("Found {} recent transactions", transactions.size());
        return transactions.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    /**
     * Get transactions expiring within a specific date range
     */
    public List<TransactionDto> getExpiringTransactions(LocalDateTime startDate, LocalDateTime endDate) {
        logger.debug("Getting transactions expiring between {} and {}", startDate, endDate);
        List<Transaction> transactions = transactionRepository.findExpiringTransactions(startDate, endDate);
        logger.debug("Found {} expiring transactions", transactions.size());
        return transactions.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    /**
     * Convert Transaction entity to DTO
     */
    public TransactionDto convertToDto(Transaction transaction) {
        TransactionDto dto = new TransactionDto();
        dto.setTransactionId(transaction.getTransactionId());
        dto.setUserId(transaction.getUserId());

        // Get mobile number from user for this transaction
        try {
            Optional<User> user = userRepository.findById(transaction.getUserId());
            if (user.isPresent()) {
                dto.setMobileNumber(user.get().getMobileNumber());
            }
        } catch (Exception e) {
            // If user lookup fails, log but continue
            logger.error("Error finding user for transaction {}: {}",
                    transaction.getTransactionId(), e.getMessage());
        }

        // Get plan details
        Plan plan = transaction.getPlan();
        if (plan != null) {
            dto.setPlanId(plan.getPlanId());
            dto.setPlanName(plan.getDataLimit() + " Plan"); // Create a plan name from data limit

            // Format plan details
            String dataLimit = plan.getDataLimit();
            if (dataLimit != null && !dataLimit.trim().isEmpty()) {
                if (!dataLimit.contains("GB") && !dataLimit.contains("MB")) {
                    try {
                        if (dataLimit.contains(".")) {
                            Double.parseDouble(dataLimit);
                            dataLimit = dataLimit + " GB";
                        } else {
                            Integer.parseInt(dataLimit);
                            dataLimit = dataLimit + " GB";
                        }
                    } catch (NumberFormatException e) {
                        // Not a number, leave as is
                        logger.warn("Could not parse data limit as number: {}", dataLimit);
                    }
                }
            }

            dto.setData(dataLimit);
            dto.setValidity(plan.getValidityDays() != null ? plan.getValidityDays() + " days" : "- days");
            dto.setCalls("Unlimited");
            dto.setOttBenefits(plan.getOttBenefits() != null ? plan.getOttBenefits() : "No additional benefits");
        }

        dto.setAmount(transaction.getAmount());
        dto.setPaymentStatus(transaction.getPaymentStatus());
        dto.setPaymentMethod(transaction.getPaymentMethod());
        dto.setTransactionDate(transaction.getTransactionDate());
        dto.setExpiryDate(transaction.getExpiryDate());

        return dto;
    }
}