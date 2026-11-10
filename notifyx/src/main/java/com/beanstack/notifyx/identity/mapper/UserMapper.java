package com.beanstack.notifyx.identity.mapper;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.beanstack.notifyx.identity.dto.response.UserResponse;
import com.beanstack.notifyx.identity.entity.User;

@Component 
public class UserMapper {

    public UserResponse toUserResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getFullName(),
            user.getEmail(),
            user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet())
        );
    }
}
