package com.beanstack.notifyx.apikey.entity;

import java.time.Instant;
import java.util.UUID;

import com.beanstack.notifyx.identity.entity.User;
import com.beanstack.notifyx.project.entity.Project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity 
@Table (
    name = "api_keys",
    indexes = {
        @Index (name = "idx_api_keys_key_id", columnList = "key_id", unique = true),
        @Index (name = "idx_api_keys_project_id", columnList = "project_id")
    }
)
@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor 
public class ApiKey {
    
    @Id 
    @GeneratedValue (strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "project_id", nullable = false)
    private Project project;

    @Column (name = "key_id", nullable = false, unique = true, length = 16)
    private String keyId;

    @Column (name = "key_hash", nullable = false, length = 64)
    private String keyHash;

    @Column (nullable = false, length = 100)
    private String name;

    @Enumerated (jakarta.persistence.EnumType.STRING)
    @Column (nullable = false, length = 20)
    private ApiKeyStatus status;

    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "created_by", nullable = false)
    private User createdBy;

    private Instant expiresAt;

    private Instant lastUsedAt;

    private Instant revokedAt;

    @Column (nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist 
    protected void onCreate() {
        this.createdAt = Instant.now();
        if(this.status == null) {
            this.status = ApiKeyStatus.ACTIVE;
        }
    }
}
