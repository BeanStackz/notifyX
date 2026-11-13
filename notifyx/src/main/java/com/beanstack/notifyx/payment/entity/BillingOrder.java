package com.beanstack.notifyx.payment.entity;

import java.time.Instant;
import java.util.UUID;

import com.beanstack.notifyx.organization.entity.Organization;
import com.beanstack.notifyx.subscription.entity.Plan;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "billing_orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(name = "razorpay_order_id", unique = true)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id", unique = true)
    private String razorpayPaymentId;

    @Column(nullable = false)
    private long amountInPaise;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BillingOrderStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant paidAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
