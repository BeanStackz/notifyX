package com.beanstack.notifyx.subscription.config;

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.beanstack.notifyx.notification.entity.NotificationChannel;
import com.beanstack.notifyx.notification.entity.NotificationPriority;
import com.beanstack.notifyx.subscription.entity.Plan;
import com.beanstack.notifyx.subscription.repository.PlanRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PlanInitializer implements CommandLineRunner {

    private final PlanRepository planRepository;

    @Value("${razorpay.plan-id.starter:}")
    private String starterRazorpayPlanId;

    @Value("${razorpay.plan-id.pro:}")
    private String proRazorpayPlanId;

    @Value("${razorpay.plan-id.enterprise:}")
    private String enterpriseRazorpayPlanId;

    @Override
    @Transactional
    public void run(String... args) {

        createPlan(
            "FREE", "Free",
            "For testing and small projects",
            0, 1_000, 10,
            NotificationPriority.LOW,
            "",
            Set.of(
                NotificationChannel.EMAIL,
                NotificationChannel.IN_APP
            )
        );

        createPlan(
            "STARTER", "Starter",
            "For small applications",
            49900, 10_000, 60,
            NotificationPriority.NORMAL,
            starterRazorpayPlanId,
            Set.of(
                NotificationChannel.EMAIL,
                NotificationChannel.SMS,
                NotificationChannel.IN_APP
            )
        );

        createPlan(
            "PRO", "Professional",
            "For growing applications",
            199900, 100_000, 300,
            NotificationPriority.HIGH,
            proRazorpayPlanId,
            Set.of(
                NotificationChannel.EMAIL,
                NotificationChannel.SMS,
                NotificationChannel.PUSH,
                NotificationChannel.IN_APP
            )
        );

        createPlan(
            "ENTERPRISE", "Enterprise",
            "For high-volume applications",
            999900, 1_000_000, 1_000,
            NotificationPriority.CRITICAL,
            enterpriseRazorpayPlanId,
            Set.of(
                NotificationChannel.EMAIL,
                NotificationChannel.SMS,
                NotificationChannel.PUSH,
                NotificationChannel.IN_APP
            )
        );
    }

    private void createPlan(
        String code,
        String name,
        String description,
        long priceInPaise,
        long quota,
        int rateLimit,
        NotificationPriority priority,
        String razorpayPlanId,
        Set<NotificationChannel> channels
    ) {
        Plan plan = planRepository.findByCode(code)
            .orElseGet(Plan::new);

        plan.setCode(code);
        plan.setName(name);
        plan.setDescription(description);
        plan.setMonthlyPriceInPaise(priceInPaise);
        plan.setCurrency("INR");
        plan.setMonthlyQuota(quota);
        plan.setRateLimitPerMinute(rateLimit);
        plan.setPriority(priority);
        if (razorpayPlanId != null && !razorpayPlanId.isBlank()) {
            plan.setRazorpayPlanId(razorpayPlanId.trim());
        }
        plan.setAllowedChannels(new HashSet<>(channels));
        plan.setEnabled(true);

        planRepository.save(plan);
    }
}