package com.beanstack.notifyx.authorization.service;

import com.beanstack.notifyx.authorization.entity.RoleName;
import com.beanstack.notifyx.identity.entity.User;

public interface AuthorizationService {
    
    boolean hasRole(User user, RoleName roleName);

    void assignRole(User user, RoleName roleName);

    void removeRole(User user, RoleName roleName);
}
