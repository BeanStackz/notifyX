package com.beanstack.notifyx.project.entity;

import java.time.Instant;
import java.util.UUID;

import com.beanstack.notifyx.organization.entity.Organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity 
@Data 
@Table (
    name = "projects",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_project_organization_slug", 
            columnNames = {"organization_id", "slug"}
        ),
    },
    indexes = {
        @jakarta.persistence.Index(
            name = "idx_projects_organization", 
            columnList = "organization_id"
        ),
    }
)
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
public class Project {

    @Id 
    @GeneratedValue (strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne (
        fetch = jakarta.persistence.FetchType.LAZY, 
        optional = false
    )
    @JoinColumn (name = "organization_id", nullable = false)
    private Organization organization;

    @Column (nullable = false, length = 150)
    private String name;

    @Column (nullable = false, length = 100)
    private String slug;

    @Column (length = 500)
    private String description;
    
    @Column (nullable = false, length = 30)
    @Enumerated (jakarta.persistence.EnumType.STRING)
    @Builder.Default
    private ProjectStatus status = ProjectStatus.ACTIVE;

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
