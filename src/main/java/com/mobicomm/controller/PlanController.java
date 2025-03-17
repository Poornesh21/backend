package com.mobicomm.controller;

import com.mobicomm.dto.PlanDto;
import com.mobicomm.entity.PlanCategory;
import com.mobicomm.exception.ResourceNotFoundException;
import com.mobicomm.repository.PlanCategoryRepository;
import com.mobicomm.service.PlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Public Plan Controller
 * Provides read-only access to active plans
 */
@RestController
@RequestMapping("/api/plans")
@CrossOrigin(origins = "*",
        methods = {RequestMethod.GET, RequestMethod.OPTIONS},
        allowedHeaders = "*")
public class PlanController {
    @Autowired
    private PlanService planService;

    /**
     * Get all active plans
     */
    @GetMapping
    public ResponseEntity<List<PlanDto>> getAllActivePlans(
            @RequestParam(required = false) Integer categoryId) {

        List<PlanDto> plans = planService.getAllActivePlans();

        // Filter by category if categoryId is provided
        if (categoryId != null) {
            plans = plans.stream()
                    .filter(plan -> plan.getCategoryId().equals(categoryId))
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(plans);
    }

    /**
     * Get a specific active plan by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<PlanDto> getPlanById(@PathVariable Integer id) {
        PlanDto plan = planService.getPlanById(id);

        // Only return the plan if it's active
        if (!plan.getIsActive()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(plan);
    }
}

