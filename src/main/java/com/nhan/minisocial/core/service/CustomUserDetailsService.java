package com.nhan.minisocial.core.service;

import com.nhan.minisocial.core.entity.UserEntity;
import com.nhan.minisocial.core.repository.UserRepository;
import com.nhan.minisocial.core.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import javax.transaction.Transactional;

public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail).orElseThrow(
                () -> new UsernameNotFoundException("User not found with username or email: "+ usernameOrEmail)
        );
        return UserPrincipal.create(user);
    }

    @Transactional
    public UserDetails loadUserById(long id){
        UserEntity user = userRepository.findById(id).orElseThrow(
                () -> new UsernameNotFoundException("User not found with id: "+ id)
        );
        return UserPrincipal.create(user);
    }
}
