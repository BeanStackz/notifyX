package com.beanstack.notifyx.authorization.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity 
@Table (name = "roles")
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
public class Role {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated (EnumType.STRING)
    @Column (unique = true, nullable = false, length = 50)
    private RoleName name;
}
