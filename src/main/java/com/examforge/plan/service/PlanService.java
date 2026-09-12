package com.examforge.plan.service;

import com.examforge.plan.dto.PlanRequest;
import com.examforge.plan.dto.PlanResponse;

import java.util.List;
import java.util.UUID;

public interface PlanService {

    List<PlanResponse> getActivePlans();

    List<PlanResponse> getAllPlans();

    PlanResponse getById(UUID id);

    PlanResponse create(PlanRequest request);

    PlanResponse update(UUID id, PlanRequest request);

    void delete(UUID id);
}
