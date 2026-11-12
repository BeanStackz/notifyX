package com.beanstack.notifyx.outbox.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.beanstack.notifyx.outbox.entity.OutboxEvent;

public interface OutboxEventRepository
    extends JpaRepository<OutboxEvent, UUID> {
}