package com.beanstack.notifyx.payment.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.beanstack.notifyx.payment.entity.BillingOrder;

import jakarta.persistence.LockModeType;

public interface BillingOrderRepository extends JpaRepository<BillingOrder, UUID> {
    Optional<BillingOrder> findByRazorpayOrderId(String razorpayOrderId);

    Optional<BillingOrder> findByIdAndOrganization_Id(UUID id, UUID organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from BillingOrder o where o.id = :id")
    Optional<BillingOrder> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByRazorpayPaymentId(String razorpayPaymentId);
}
