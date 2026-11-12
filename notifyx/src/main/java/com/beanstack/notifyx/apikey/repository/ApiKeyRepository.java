package com.beanstack.notifyx.apikey.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.beanstack.notifyx.apikey.entity.ApiKey;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    Optional<ApiKey> findByKeyId(String keyId);

    List<ApiKey> findAllByProjectIdOrderByCreatedAtDesc(UUID projectId);

    Optional<ApiKey> findByIdAndProjectId(UUID id, UUID projectId);

    @Modifying
    @Query("""
        UPDATE ApiKey a
        SET a.lastUsedAt = :now
        WHERE a.id = :apiKeyId
        AND (
            a.lastUsedAt IS NULL
            OR a.lastUsedAt < :threshold
        )
    """)
    int updateLastUsedAtIfOlder(
        @Param("apiKeyId") UUID apiKeyId,
        @Param("now") Instant now,
        @Param("threshold") Instant threshold
    );
}
