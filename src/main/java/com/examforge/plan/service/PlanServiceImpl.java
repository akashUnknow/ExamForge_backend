package com.examforge.plan.service;

import com.examforge.common.exception.BusinessException;
import com.examforge.common.exception.DuplicateResourceException;
import com.examforge.common.exception.ResourceInUseException;
import com.examforge.common.exception.ResourceNotFoundException;
import com.examforge.plan.domain.Plan;
import com.examforge.plan.domain.PlanType;
import com.examforge.plan.dto.PlanRequest;
import com.examforge.plan.dto.PlanResponse;
import com.examforge.plan.repository.PlanRepository;
import com.examforge.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanServiceImpl implements PlanService {

    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PlanResponse> getActivePlans() {
        return planRepository.findByActiveTrue().stream().map(PlanResponse::from).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanResponse> getAllPlans() {
        return planRepository.findAll().stream().map(PlanResponse::from).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PlanResponse getById(UUID id) {
        return PlanResponse.from(findEntity(id));
    }

    @Override
    @Transactional
    public PlanResponse create(PlanRequest request) {
        String name = request.getName().trim();
        if (planRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("A plan named '" + name + "' already exists");
        }
        validatePricing(request);

        Plan plan = new Plan();
        plan.setName(name);
        applyRequest(plan, request);

        return PlanResponse.from(planRepository.save(plan));
    }

    @Override
    @Transactional
    public PlanResponse update(UUID id, PlanRequest request) {
        Plan plan = findEntity(id);
        String name = request.getName().trim();

        if (!name.equalsIgnoreCase(plan.getName()) && planRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("A plan named '" + name + "' already exists");
        }
        validatePricing(request);

        plan.setName(name);
        applyRequest(plan, request);

        return PlanResponse.from(planRepository.save(plan));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Plan plan = findEntity(id);

        if (subscriptionRepository.existsByPlanId(id)) {
            throw new ResourceInUseException("Cannot delete this plan while subscriptions reference it - deactivate it instead");
        }

        plan.setDeletedAt(Instant.now());
        planRepository.save(plan);
    }

    private void validatePricing(PlanRequest request) {
        if (request.getPlanType() != PlanType.FREE && request.getPrice() == null) {
            throw new BusinessException("Price is required for a non-FREE plan", HttpStatus.BAD_REQUEST);
        }
    }

    private void applyRequest(Plan plan, PlanRequest request) {
        plan.setPlanType(request.getPlanType());
        plan.setPrice(request.getPlanType() == PlanType.FREE ? null : request.getPrice());
        plan.setDurationDays(request.getDurationDays());
        plan.setDescription(request.getDescription());
        plan.setActive(request.isActive());
    }

    private Plan findEntity(UUID id) {
        return planRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Plan", id));
    }
}
