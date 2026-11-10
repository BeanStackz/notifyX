package com.beanstack.notifyx.authorization.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.beanstack.notifyx.authorization.entity.Role;
import com.beanstack.notifyx.authorization.entity.RoleName;
import com.beanstack.notifyx.authorization.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

@Configuration 
@RequiredArgsConstructor
public class RoleInitializer {

    private final RoleRepository roleRepository;
    
    

    @Bean 
    public CommandLineRunner initializeRoles() {
        return args -> {
            for(RoleName roleName : RoleName.values()) {
                roleRepository.findByName(roleName)
                    .orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));
            }
        };
    }
    
}
