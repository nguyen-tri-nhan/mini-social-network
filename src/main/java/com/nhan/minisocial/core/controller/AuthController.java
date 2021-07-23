package com.nhan.minisocial.core.controller;


import com.nhan.minisocial.core.api.Api;
import com.nhan.minisocial.core.entity.Role;
import com.nhan.minisocial.core.entity.RoleName;
import com.nhan.minisocial.core.entity.User;
import com.nhan.minisocial.core.exception.AppException;
import com.nhan.minisocial.core.payload.ApiResponse;
import com.nhan.minisocial.core.payload.JwtAuthenticationResponse;
import com.nhan.minisocial.core.payload.LoginRequest;
import com.nhan.minisocial.core.payload.SignUpRequest;
import com.nhan.minisocial.core.repository.RoleRepository;
import com.nhan.minisocial.core.repository.UserRepository;
import com.nhan.minisocial.core.security.CurrentUser;
import com.nhan.minisocial.core.security.JwtTokenProvider;
import com.nhan.minisocial.core.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;
import java.util.Collections;

@RestController
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/api/auth/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtTokenProvider.generateToken(authentication);

        return ResponseEntity.ok(new JwtAuthenticationResponse(jwt));
    }

    @PostMapping("/api/auth/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest signUpRequest){
        if(userRepository.existsByUsername(signUpRequest.getUsername())){
            return new ResponseEntity(new ApiResponse(false, "Username is already taken!"),
                    HttpStatus.BAD_REQUEST);
        }
        if(userRepository.existsByEmail(signUpRequest.getEmail())) {
            return new ResponseEntity(new ApiResponse(false, "Email Address already in use!"),
                    HttpStatus.BAD_REQUEST);
        }
        //create user
        User user = new User(signUpRequest.getUsername(),
                signUpRequest.getFirstname(),
                signUpRequest.getLastname(),
                signUpRequest.getPassword(),
                signUpRequest.getEmail(),
                signUpRequest.getAvatar());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER).orElseThrow(
                () -> new AppException("User Role Not set")
        );

        user.setRoles(Collections.singleton(userRole));

        User result = userRepository.save(user);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/api/users/{username}")
                .buildAndExpand(result.getUsername()).toUri();
        return ResponseEntity.created(location).body(new ApiResponse(true, "User registered successfully"));
    }

    @GetMapping(Api.User.GET_ME)
    public UserPrincipal getMe(@CurrentUser UserPrincipal user) {
        return user;
    }

}
