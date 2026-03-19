package com.validation.auth.backend.services;

import com.validation.auth.backend.entities.User;

public interface EmailVerificationService {

    void sendVerificationEmail(User user);

    void verify(String token);

    void resendByEmail(String email);
}
