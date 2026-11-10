package com.beanstack.notifyx.authorization.service.impl;

import org.springframework.stereotype.Service;

import com.beanstack.notifyx.authorization.entity.Role;
import com.beanstack.notifyx.authorization.entity.RoleName;
import com.beanstack.notifyx.authorization.repository.RoleRepository;
import com.beanstack.notifyx.authorization.service.AuthorizationService;
import com.beanstack.notifyx.identity.entity.User;
import com.beanstack.notifyx.identity.repository.UserRepository;
import com.beanstack.notifyx.shared.exception.ResourceNotFoundException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class AuthorizationServiceImpl implements AuthorizationService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    @Override
    public boolean hasRole(User user, RoleName roleName) {
        return user.getRoles().stream()
            .anyMatch(role -> role.getName() == roleName);
    }

    @Override
    @Transactional 
    public void assignRole(User user, RoleName roleName) {
        User managedUser = userRepository.findById(user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Role role = roleRepository.findByName(roleName)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
    
        managedUser.getRoles().add(role);
    }

    @Override
    @Transactional 
    public void removeRole(User user, RoleName roleName) {

        User managedUser = userRepository.findById(user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    
        managedUser.getRoles().removeIf(role -> role.getName() == roleName);
    }
    
}
