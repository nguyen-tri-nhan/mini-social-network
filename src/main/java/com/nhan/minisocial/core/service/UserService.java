package com.nhan.minisocial.core.service;

import com.nhan.minisocial.core.entity.User;
import com.nhan.minisocial.core.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository repository;

    public User getUser(long id){
        return repository.getOne(id);
    }
}
