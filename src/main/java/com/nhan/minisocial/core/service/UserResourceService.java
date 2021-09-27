package com.nhan.minisocial.core.service;

import com.nhan.minisocial.core.entity.User;
import com.nhan.minisocial.core.resource.UserResource;
import com.nhan.minisocial.core.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserResourceService {

    @Autowired
    private UserService userService;


    public UserResource toResource(User user){
        UserResource userResource = new UserResource();
        userResource.setId(user.getId());
        userResource.setAvatar(user.getAvatar());
        userResource.setEmail(user.getEmail());
        userResource.setFullname(user.getFirstname() + " " + user.getLastname());
        return userResource;
    }

    public UserResource toResource(UserPrincipal userPrincipal) {
        UserResource userResource = new UserResource();
        userResource.setId(userPrincipal.getId());
        userResource.setFullname(userPrincipal.getFullname());
        userResource.setAvatar(userPrincipal.getAvatar());
        userResource.setEmail(userPrincipal.getEmail());
        return userResource;
    }

    public UserResource getUser(long id){
        User user = userService.getUser(id);
        return toResource(user);
    }
}
