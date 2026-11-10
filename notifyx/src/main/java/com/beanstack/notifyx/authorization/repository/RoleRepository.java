package com.beanstack.notifyx.authorization.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.beanstack.notifyx.authorization.entity.Role;
import com.beanstack.notifyx.authorization.entity.RoleName;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    
    Optional<Role> findByName(RoleName name);
}
