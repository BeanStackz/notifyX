package com.beanstack.notifyx.subscription.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beanstack.notifyx.subscription.dto.response.PlanResponse;
import com.beanstack.notifyx.subscription.entity.Plan;
import com.beanstack.notifyx.subscription.repository.PlanRepository;
import com.beanstack.notifyx.subscription.service.PlanService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlanServiceImpl implements PlanService {

    private final PlanRepository planRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PlanResponse> getAvailablePlans() {

        return planRepository.findAllByEnabledTrueOrderByMonthlyQuotaAsc()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private PlanResponse toResponse(Plan plan) {
        return new PlanResponse(
            plan.getId(),
            plan.getCode(),
            plan.getName(),
            plan.getDescription(),
            plan.getMonthlyPriceInPaise(),
            plan.getCurrency(),
            plan.getMonthlyQuota(),
            plan.getRateLimitPerMinute(),
            plan.getPriority(),
            plan.getAllowedChannels()
        );
    }
}