package com.beanstack.notifyx.organization.entity;

import java.time.Instant;
import java.util.UUID;

import com.beanstack.notifyx.identity.entity.User;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity 
@Table (
    name = "organization_members",
    uniqueConstraints = {
        @UniqueConstraint (
            name = "uk_organization_member",
            columnNames = {"organization_id", "user_id"}
        )
    },
    indexes = {
        @Index(name = "idx_org_members_organization", columnList = "organization_id"),
        @Index(name = "idx_org_members_user", columnList = "user_id")
    }
)
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
public class OrganizationMember {
    

    @Id 
    @GeneratedValue (strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "user_id", nullable = false)
    private User user;

    @Enumerated (jakarta.persistence.EnumType.STRING)
    @Column (nullable = false, length = 50)
    private OrganizationRole role;

    @Column (nullable = false, updatable = false)
    private Instant joinedAt;

    @PrePersist 
    protected void onCreate() {
        this.joinedAt = Instant.now();
    }
}
