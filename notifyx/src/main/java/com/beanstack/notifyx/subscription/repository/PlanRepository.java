package com.beanstack.notifyx.subscription.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.beanstack.notifyx.subscription.entity.Plan;

public interface PlanRepository extends JpaRepository<Plan, UUID> {

    Optional<Plan> findByCodeAndEnabledTrue(String code);

    Optional<Plan> findByCode(String code);

    List<Plan> findAllByEnabledTrueOrderByMonthlyQuotaAsc();

    boolean existsByCode(String code);
}