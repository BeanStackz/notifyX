package com.beanstack.notifyx.subscription.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SelectPlanRequest(

    @NotBlank(message = "Plan code is required")
    @Size(max = 30)
    String planCode

) {}