package com.validation.auth.backend.services;

import com.validation.auth.backend.dtos.UserDto;

public interface AuthService {
    //register user
    UserDto registerUser(UserDto userDto);

    void verifyEmail(String token);

    void resendVerificationEmail(String email);

    void forgotPassword(String email);

    void resetPassword(String token, String newPassword);
}
