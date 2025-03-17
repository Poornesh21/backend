// PlanCategoryRepository.java
package com.mobicomm.repository;

import com.mobicomm.entity.PlanCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlanCategoryRepository extends JpaRepository<PlanCategory, Integer> {
    // Add a query to find by name (case insensitive) with correct case-sensitive table name
    @Query(value = "SELECT * FROM Plan_Categories WHERE LOWER(category_name) = LOWER(?1)", nativeQuery = true)
    Optional<PlanCategory> findByCategoryNameIgnoreCase(String categoryName);
}