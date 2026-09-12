package com.examforge.plan.repository;

import com.examforge.plan.domain.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlanRepository extends JpaRepository<Plan, UUID> {

    List<Plan> findByActiveTrue();

    boolean existsByNameIgnoreCase(String name);
}
