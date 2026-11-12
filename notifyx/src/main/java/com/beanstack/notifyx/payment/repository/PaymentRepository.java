package com.beanstack.notifyx.payment.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.beanstack.notifyx.payment.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    boolean existsByRazorpayPaymentId(String razorpayPaymentId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);
}