package com.beanstack.notifyx.subscription.service;

import java.util.List;

import com.beanstack.notifyx.subscription.dto.response.PlanResponse;

public interface PlanService {

    List<PlanResponse> getAvailablePlans();
}