package com.mobicomm.service;

import com.mobicomm.dto.PlanDto;
import com.mobicomm.entity.Plan;
import com.mobicomm.entity.PlanCategory;
import com.mobicomm.exception.ResourceNotFoundException;
import com.mobicomm.repository.PlanRepository;
import com.mobicomm.repository.PlanCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlanService {
    private static final Logger logger = LoggerFactory.getLogger(PlanService.class);

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private PlanCategoryRepository planCategoryRepository;

    // Convert Plan entity to PlanDto with proper data limit formatting
    private PlanDto convertToDto(Plan plan) {
        PlanDto planDto = new PlanDto();

        planDto.setPlanId(plan.getPlanId());
        planDto.setCategoryId(plan.getCategoryId());
        planDto.setPrice(plan.getPrice());

        // Any case variant of "Active" will result in active = true.
        planDto.setIsActive("Active".equalsIgnoreCase(plan.getStatus()));

        // Format the data limit with GB suffix if not already present
        String dataLimit = plan.getDataLimit();
        if (dataLimit != null && !dataLimit.trim().isEmpty()) {
            if (!dataLimit.contains("GB") && !dataLimit.contains("MB")) {
                // Try to determine if it's a number
                try {
                    // If it parses as a number, add GB suffix
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
        planDto.setData(dataLimit);

        planDto.setValidity(plan.getValidityDays() != null ? plan.getValidityDays() + " days" : "- days");
        planDto.setCalls("Unlimited");
        planDto.setBenefits(plan.getOttBenefits() != null ? plan.getOttBenefits() : "No additional benefits");

        if (plan.getCategoryId() != null) {
            PlanCategory category = planCategoryRepository.findById(plan.getCategoryId())
                    .orElse(null);
            planDto.setCategoryName(category != null ? category.getCategoryName() : "Uncategorized");
        } else {
            planDto.setCategoryName("Uncategorized");
        }
        return planDto;
    }

    // Parse data limit to store in database (remove GB/MB suffix)
    private String parseDataLimit(String dataLimit) {
        if (dataLimit == null || dataLimit.trim().isEmpty()) {
            return null;
        }

        // Remove GB or MB suffix for storage
        dataLimit = dataLimit.trim();
        if (dataLimit.toUpperCase().endsWith(" GB")) {
            dataLimit = dataLimit.substring(0, dataLimit.length() - 3).trim();
        } else if (dataLimit.toUpperCase().endsWith("GB")) {
            dataLimit = dataLimit.substring(0, dataLimit.length() - 2).trim();
        } else if (dataLimit.toUpperCase().endsWith(" MB")) {
            dataLimit = dataLimit.substring(0, dataLimit.length() - 3).trim();
        } else if (dataLimit.toUpperCase().endsWith("MB")) {
            dataLimit = dataLimit.substring(0, dataLimit.length() - 2).trim();
        }

        return dataLimit;
    }

    public List<PlanDto> getAllPlans() {
        List<Plan> allPlans = planRepository.findAll();
        return allPlans.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public List<PlanDto> getAllActivePlans() {
        List<Plan> activePlans = planRepository.findByStatus("Active");
        return activePlans.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public List<PlanDto> getActivePlansByCategory(Integer categoryId) {
        // First check if the category exists
        planCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("PlanCategory", categoryId));

        // Get all active plans and filter by category
        return getAllActivePlans().stream()
                .filter(plan -> plan.getCategoryId().equals(categoryId))
                .collect(Collectors.toList());
    }

    public PlanDto getPlanById(Integer id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", id));
        return convertToDto(plan);
    }

    public PlanDto createPlan(PlanDto planDto) {
        if (planDto.getCategoryId() != null) {
            planCategoryRepository.findById(planDto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("PlanCategory", planDto.getCategoryId()));
        } else {
            throw new IllegalArgumentException("Category ID is required");
        }

        Plan plan = new Plan();
        plan.setCategoryId(planDto.getCategoryId());
        plan.setPrice(planDto.getPrice() != null ? planDto.getPrice() : BigDecimal.ZERO);

        if (planDto.getValidity() != null && !planDto.getValidity().isEmpty()) {
            try {
                String validityValue = planDto.getValidity().replace(" days", "").trim();
                plan.setValidityDays(!validityValue.equals("-") ? Integer.parseInt(validityValue) : 30);
            } catch (Exception e) {
                logger.warn("Failed to parse validity: {}", planDto.getValidity());
                plan.setValidityDays(30);
            }
        } else {
            plan.setValidityDays(30);
        }

        // Parse data limit (remove GB/MB suffix)
        plan.setDataLimit(parseDataLimit(planDto.getData()));

        plan.setOttBenefits(planDto.getBenefits() != null && !planDto.getBenefits().equals("No additional benefits")
                ? planDto.getBenefits() : null);
        // Default status set to Active.
        plan.setStatus("Active");

        Plan savedPlan = planRepository.save(plan);
        return convertToDto(savedPlan);
    }

    public PlanDto updatePlan(Integer id, PlanDto planDto) {
        Plan existingPlan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", id));

        if (planDto.getCategoryId() != null && !planDto.getCategoryId().equals(existingPlan.getCategoryId())) {
            planCategoryRepository.findById(planDto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("PlanCategory", planDto.getCategoryId()));
            existingPlan.setCategoryId(planDto.getCategoryId());
        }

        if (planDto.getPrice() != null) {
            existingPlan.setPrice(planDto.getPrice());
        }
        if (planDto.getValidity() != null && !planDto.getValidity().isEmpty()) {
            try {
                String validityValue = planDto.getValidity().replace(" days", "").trim();
                if (!validityValue.equals("-")) {
                    existingPlan.setValidityDays(Integer.parseInt(validityValue));
                }
            } catch (Exception e) {
                logger.warn("Failed to parse validity: {}", planDto.getValidity());
            }
        }
        if (planDto.getData() != null) {
            existingPlan.setDataLimit(parseDataLimit(planDto.getData()));
        }
        if (planDto.getBenefits() != null) {
            existingPlan.setOttBenefits(planDto.getBenefits().equals("No additional benefits") ? null : planDto.getBenefits());
        }
        if (planDto.getIsActive() != null) {
            existingPlan.setStatus(planDto.getIsActive() ? "Active" : "Inactive");
        }

        Plan updatedPlan = planRepository.save(existingPlan);
        return convertToDto(updatedPlan);
    }

    // Toggle the plan's status (Active <-> Inactive) using a case-insensitive check.
    public PlanDto togglePlanStatus(Integer id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", id));

        plan.setStatus("Active".equalsIgnoreCase(plan.getStatus()) ? "Inactive" : "Active");
        Plan updatedPlan = planRepository.save(plan);
        return convertToDto(updatedPlan);
    }
}