package com.mobicomm.controller;

import com.mobicomm.dto.PlanDto;
import com.mobicomm.service.PlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin Plan Controller
 * Provides management operations for plans, restricted to users with ADMIN role
 */
@RestController
@RequestMapping("/api/admin/plans")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.OPTIONS},
        allowedHeaders = "*")
public class AdminPlanController {
    @Autowired
    private PlanService planService;

    /**
     * Get all plans (including inactive ones)
     */
    @GetMapping
    public ResponseEntity<List<PlanDto>> getAllPlans() {
        return ResponseEntity.ok(planService.getAllPlans());
    }

    /**
     * Get a specific plan by ID (including inactive ones)
     */
    @GetMapping("/{id}")
    public ResponseEntity<PlanDto> getPlanById(@PathVariable Integer id) {
        return ResponseEntity.ok(planService.getPlanById(id));
    }

    /**
     * Create a new plan
     */
    @PostMapping
    public ResponseEntity<PlanDto> createPlan(@RequestBody PlanDto planDto) {
        return ResponseEntity.ok(planService.createPlan(planDto));
    }

    /**
     * Update an existing plan
     */
    @PutMapping("/{id}")
    public ResponseEntity<PlanDto> updatePlan(
            @PathVariable Integer id,
            @RequestBody PlanDto planDto
    ) {
        return ResponseEntity.ok(planService.updatePlan(id, planDto));
    }

    /**
     * Toggle a plan's active status (activate/deactivate)
     */
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<PlanDto> togglePlanStatus(@PathVariable Integer id) {
        return ResponseEntity.ok(planService.togglePlanStatus(id));
    }
}