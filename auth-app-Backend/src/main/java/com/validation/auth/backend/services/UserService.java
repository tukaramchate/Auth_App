package com.validation.auth.backend.services;

import java.util.List;

import org.springframework.data.domain.Page;

import com.validation.auth.backend.dtos.UserSelfUpdateRequest;
import com.validation.auth.backend.dtos.UserDto;

public interface UserService {

    //create user
    UserDto createUser(UserDto userDto);

    //get user by email
    UserDto getUserByEmail(String email);

    //update user
    UserDto updateUser(UserDto userDto, String userId);

    //update current user profile
    UserDto updateCurrentUser(String email, UserSelfUpdateRequest request);

    //delete user
    void deleteUser(String userId);

    //get user by id
    UserDto getUserById(String userId);

    //get all users
    Iterable<UserDto> getAllUsers();

    Page<UserDto> getUsersPage(String q, Boolean enabled, String role, int page, int size, String sort);

    //admin update user status and roles
    UserDto updateUserAdmin(String userId, UserDto userDto);

    List<String> getSystemRoleNames();

}
