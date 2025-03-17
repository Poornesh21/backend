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
     */
    @Transactional
    public TransactionDto processRecharge(RechargeRequest request) {
        // Validate planId is not null
        if (request.getPlanId() == null) {
            throw new IllegalArgumentException("Plan ID cannot be null");
        }

        // Find or create user for this mobile number
        User user = findOrCreateUser(request.getMobileNumber());

        // Validate plan exists
        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan", request.getPlanId()));

        // Create transaction entity - Note we're explicitly setting all required fields
        Transaction transaction = new Transaction();
        transaction.setUserId(user.getUserId()); // This was causing the "id must not be null" error
        transaction.setPlan(plan);
        transaction.setAmount(request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO);

        // Set status - Using one of the ENUM values defined in the database schema
        // ENUM('Pending', 'Completed', 'Failed')
        transaction.setPaymentStatus(request.getPaymentStatus() != null ?
                request.getPaymentStatus() : "Completed"); // Using exact ENUM value with correct case

        transaction.setPaymentMethod(request.getPaymentMethod());

        // Set transaction date - Use current time if not provided
        transaction.setTransactionDate(request.getTransactionDate() != null ?
                request.getTransactionDate() : LocalDateTime.now());

        // Set expiry date - Calculate based on plan validity or use provided value
        LocalDateTime expiryDate;
        if (request.getExpiryDate() != null) {
            expiryDate = request.getExpiryDate();
        } else if (plan.getValidityDays() != null && plan.getValidityDays() > 0) {
            expiryDate = LocalDateTime.now().plusDays(plan.getValidityDays());
        } else {
            // Default to 30 days if no validity specified
            expiryDate = LocalDateTime.now().plusDays(30);
        }
        transaction.setExpiryDate(expiryDate);

        // Update user's last recharge date
        user.setLastRechargeDate(java.sql.Timestamp.valueOf(transaction.getTransactionDate()));
        userRepository.save(user);

        // Log what we're about to save
        System.out.println("Saving transaction: " + transaction);

        // Save transaction
        Transaction savedTransaction = transactionRepository.save(transaction);

        // Convert to DTO
        TransactionDto transactionDto = convertToDto(savedTransaction);

        // If user has an email, send confirmation and invoice
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            try {
                emailService.sendPaymentConfirmationEmail(
                        user.getEmail(),
                        user.getMobileNumber(),
                        transactionDto.getPlanName() != null ? transactionDto.getPlanName() : plan.getDataLimit() + " Plan",
                        transaction.getAmount().toString(),
                        transaction.getTransactionId().toString()
                );

                // Also send invoice
                emailService.sendInvoiceEmail(
                        user.getEmail(),
                        user.getMobileNumber(),
                        transactionDto.getPlanName() != null ? transactionDto.getPlanName() : plan.getDataLimit() + " Plan",
                        transaction.getAmount().toString(),
                        transaction.getTransactionId().toString(),
                        transaction.getPaymentMethod(),
                        transaction.getTransactionDate().toString()
                );
            } catch (Exception e) {
                // Log but don't fail if email sending fails
                System.err.println("Failed to send email: " + e.getMessage());
            }
        }

        return transactionDto;
    }

    /**
     * Find existing user or create a new user record if not found
     */
    private User findOrCreateUser(String mobileNumber) {
        Optional<User> userOpt = userRepository.findByMobileNumber(mobileNumber);

        if (userOpt.isPresent()) {
            return userOpt.get();
        } else {
            // Create a new user for this mobile number
            User newUser = new User();
            newUser.setMobileNumber(mobileNumber);
            // Set default role for new users
            newUser.setRoleId(2); // Assuming 2 is the regular user role ID

            return userRepository.save(newUser);
        }
    }

    /**
     * Get transactions by mobile number
     */
    public List<TransactionDto> getTransactionsByMobileNumber(String mobileNumber) {
        Optional<User> user = userRepository.findByMobileNumber(mobileNumber);

        if (user.isPresent()) {
            List<Transaction> transactions = transactionRepository.findByUserIdOrderByTransactionDateDesc(user.get().getUserId());
            return transactions.stream().map(this::convertToDto).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    /**
     * Get transactions by user ID
     */
    public List<TransactionDto> getTransactionsByUserId(Integer userId) {
        List<Transaction> transactions = transactionRepository.findByUserIdOrderByTransactionDateDesc(userId);
        return transactions.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    /**
     * Get transaction by ID
     */
    public TransactionDto getTransactionById(Integer id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
        return convertToDto(transaction);
    }

    /**
     * Get transaction by payment reference
     */
    public TransactionDto getTransactionByReference(String reference) {
        Optional<Transaction> transaction = transactionRepository.findByPaymentReference(reference);
        return transaction.map(this::convertToDto).orElse(null);
    }

    /**
     * Get all transactions with optional filtering
     */
    public List<TransactionDto> getAllTransactionsWithFilters(String status, LocalDate fromDate, LocalDate toDate, Integer planId) {
        List<Transaction> transactions;

        if (status != null || fromDate != null || toDate != null || planId != null) {
            // Convert dates to LocalDateTime for comparison
            LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
            LocalDateTime toDateTime = toDate != null ? toDate.atTime(LocalTime.MAX) : null;

            transactions = transactionRepository.findWithFilters(status, fromDateTime, toDateTime, planId);
        } else {
            transactions = transactionRepository.findAll();
        }

        return transactions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get transaction statistics
     */
    public Map<String, Object> getTransactionStatistics(LocalDate fromDate, LocalDate toDate) {
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

        return statistics;
    }

    /**
     * Get recent transactions (last X days)
     */
    public List<TransactionDto> getRecentTransactions(Integer days) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
        List<Transaction> transactions = transactionRepository.findByTransactionDateAfterOrderByTransactionDateDesc(fromDate);
        return transactions.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    /**
     * Get transactions expiring within a specific date range
     */
    public List<TransactionDto> getExpiringTransactions(LocalDateTime startDate, LocalDateTime endDate) {
        List<Transaction> transactions = transactionRepository.findExpiringTransactions(startDate, endDate);
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
            System.err.println("Error finding user for transaction: " + e.getMessage());
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