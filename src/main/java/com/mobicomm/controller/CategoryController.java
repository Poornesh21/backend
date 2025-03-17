package com.mobicomm.controller;
import com.mobicomm.entity.PlanCategory;
import com.mobicomm.exception.ResourceNotFoundException;
import com.mobicomm.repository.PlanCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*",
        methods = {RequestMethod.GET, RequestMethod.OPTIONS},
        allowedHeaders = "*")
public class CategoryController {

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
}