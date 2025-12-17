package com.validation.auth.backend.services;

import com.validation.auth.backend.dtos.UserDto;

public interface AuthService {
    //register user
    UserDto registerUser(UserDto userDto);
    //login user
}
