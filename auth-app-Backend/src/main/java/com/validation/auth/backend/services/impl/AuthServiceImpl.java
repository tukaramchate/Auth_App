package com.validation.auth.backend.services.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.validation.auth.backend.dtos.UserDto;
import com.validation.auth.backend.entities.Provider;
import com.validation.auth.backend.services.AuthService;
import com.validation.auth.backend.services.EmailVerificationService;
import com.validation.auth.backend.services.UserService;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    @Value("${app.auth.verification.enabled:true}")
    private boolean emailVerificationEnabled;

    @Override
    public UserDto registerUser(UserDto userDto) {
        userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));
        userDto.setProvider(Provider.LOCAL);
        userDto.setEnable(!emailVerificationEnabled);

        UserDto createdUser = userService.createUser(userDto);
        if (emailVerificationEnabled) {
            emailVerificationService.resendByEmail(createdUser.getEmail());
        }
        return createdUser;
    }

    @Override
    public void verifyEmail(String token) {
        if (!emailVerificationEnabled) {
            return;
        }
        emailVerificationService.verify(token);
    }

    @Override
    public void resendVerificationEmail(String email) {
        if (!emailVerificationEnabled) {
            return;
        }
        emailVerificationService.resendByEmail(email);
    }

}
