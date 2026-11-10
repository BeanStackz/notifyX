package com.beanstack.notifyx.identity.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.beanstack.notifyx.authorization.entity.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity 
@Table (
    name = "users",
    indexes = {
        @Index (name = "idx_users_email", columnList = "email"),
    }
)
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
public class User {
    
    @Id 
    @GeneratedValue (strategy = jakarta.persistence.GenerationType.UUID)
    private UUID id;

    @Column (unique = true, nullable = false, length = 255)
    private String email;

    @Column (nullable = false)
    private String passwordHash;

    @Column (nullable = false, length = 100)
    private String fullName;

    @Column (nullable = false)
    @Builder.Default
    private boolean enabled = true;


    @ManyToMany (fetch = FetchType.EAGER)
    @JoinTable (
        name = "user_roles",
        joinColumns = @JoinColumn (name = "user_id"),
        inverseJoinColumns = @JoinColumn (name = "role_id"),
        uniqueConstraints = {
            @UniqueConstraint (
                columnNames = {"user_id", "role_id"}
            )
        }
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Column (nullable = false, updatable = false)
    private Instant createdAt;

    @Column (nullable = false)
    private Instant updatedAt;

    @PrePersist 
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
}
