package com.beanstack.notifyx.identity.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    name = "refresh_tokens",
    indexes = {
        @Index (name = "idx_refresh_tokens_user", columnList = "user_id"),
        @Index (name = "idx_refresh_tokens_family", columnList = "family_id"),
    }
)
@Data 
@AllArgsConstructor 
@NoArgsConstructor 
@Builder 
public class RefreshToken {

    @Id 
    @GeneratedValue (strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne (fetch = FetchType.LAZY,optional = false)
    @JoinColumn (name = "user_id", nullable = false)
    private User user;

    @Column (nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column (nullable = false)
    private UUID familyId;

    @Column (nullable = false)
    private Instant expiresAt;

    private Instant revokedAt;

    private Instant usedAt;

    @Column (length = 64)
    private String replacedByTokenHash;

    @Column (nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist 
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public boolean isActive() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }
    
}
