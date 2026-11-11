package com.beanstack.notifyx.organization.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity 
@Table (
    name = "organizations",
    indexes = {
        @Index(name = "idx_organizations_slug", columnList = "slug")
    }
)
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
@Data 
public class Organization {

    @Id 
    @GeneratedValue (strategy = GenerationType.UUID)
    private UUID id;

    @Column (nullable = false, length = 150)
    private String name;

    @Column (nullable = false, unique = true, length = 150)
    private String slug;

    @Column (nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column (nullable = false, updatable = false)
    private Instant createdAt;

    @Column (nullable = false)
    private Instant updatedAt;

    @PrePersist 
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate 
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
    
}
