package com.mobicomm.repository;

import com.mobicomm.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Integer> {
    // Find all plans, regardless of status
    @Query(value = "SELECT * FROM recharge_plans", nativeQuery = true)
    List<Plan> findAllPlans();

    // Keep the method to find active plans if needed
    List<Plan> findByStatus(String status);
}