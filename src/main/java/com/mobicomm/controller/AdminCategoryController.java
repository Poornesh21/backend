package com.mobicomm.controller;

import com.mobicomm.entity.PlanCategory;
import com.mobicomm.exception.ResourceNotFoundException;
import com.mobicomm.repository.PlanCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin Category Controller
 * Provides management operations for categories, restricted to users with ADMIN role
 */
@RestController
@RequestMapping("/api/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS},
        allowedHeaders = "*")
public class AdminCategoryController {

    @Autowired
    private PlanCategoryRepository planCategoryRepository;

    /**
     * Get all categories
     */
    @GetMapping
    public ResponseEntity<List<PlanCategory>> getAllCategories() {
        List<PlanCategory> categories = planCategoryRepository.findAll();
        return ResponseEntity.ok(categories);
    }

    /**
     * Get a specific category by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<PlanCategory> getCategoryById(@PathVariable Integer id) {
        PlanCategory category = planCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PlanCategory", id));
        return ResponseEntity.ok(category);
    }

    /**
     * Create a new category
     */
    @PostMapping
    public ResponseEntity<PlanCategory> createCategory(@RequestBody PlanCategory planCategory) {
        // Don't set the ID manually - let the database auto-increment handle it
        planCategory.setCategoryId(null);

        PlanCategory savedCategory = planCategoryRepository.save(planCategory);
        return ResponseEntity.ok(savedCategory);
    }

    /**
     * Update an existing category
     */
    @PutMapping("/{id}")
    public ResponseEntity<PlanCategory> updateCategory(
            @PathVariable Integer id,
            @RequestBody PlanCategory planCategoryDetails
    ) {
        PlanCategory category = planCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PlanCategory", id));

        category.setCategoryName(planCategoryDetails.getCategoryName());

        // Check if the entity has display order field
        if (hasDisplayOrderField(category) && hasDisplayOrderField(planCategoryDetails)) {
            try {
                // Use reflection to set the display order if it exists
                java.lang.reflect.Method getMethod = PlanCategory.class.getMethod("getDisplayOrder");
                java.lang.reflect.Method setMethod = PlanCategory.class.getMethod("setDisplayOrder", Integer.class);

                Integer displayOrder = (Integer) getMethod.invoke(planCategoryDetails);
                if (displayOrder != null) {
                    setMethod.invoke(category, displayOrder);
                }
            } catch (Exception e) {
                // Ignore if methods don't exist
            }
        }

        PlanCategory updatedCategory = planCategoryRepository.save(category);
        return ResponseEntity.ok(updatedCategory);
    }

    /**
     * Check if the PlanCategory class has a displayOrder field
     */
    private boolean hasDisplayOrderField(PlanCategory category) {
        try {
            category.getClass().getDeclaredField("displayOrder");
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }
}