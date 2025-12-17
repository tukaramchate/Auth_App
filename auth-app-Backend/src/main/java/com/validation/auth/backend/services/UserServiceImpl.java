package com.validation.auth.backend.services;

import com.validation.auth.backend.dtos.UserDto;
import com.validation.auth.backend.entities.Provider;
import com.validation.auth.backend.entities.User;
import com.validation.auth.backend.exceptions.ResourceNotFoundException;
import com.validation.auth.backend.repositores.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {

        if(userDto.getEmail() == null || userDto.getEmail().isBlank()){
            throw new IllegalArgumentException("Email is required");
        }

        if(userRepository.existsByEmail(userDto.getEmail())){
            throw new IllegalArgumentException("Email already exists");
        }

        User user = modelMapper.map(userDto, User.class);
        user.setProvider(userDto.getProvider() != null ? userDto.getProvider() : Provider.LOCAL);
        // role assign here to user__for authorization

        User saveduser = userRepository.save(user);
        return modelMapper.map(saveduser, UserDto.class);

    }

    @Override
    public UserDto getUserByEmail(String email) {
        User user = userRepository
               .findByEmail(email)
               .orElseThrow(()->new ResourceNotFoundException("User not found with given emial id "));
        return modelMapper.map(user, UserDto.class);
    }

    @Override
    public UserDto updateUser(UserDto userDto, String userId) {
        return null;
    }

    @Override
    public void deleteUser(UserDto userDto) {

    }

    @Override
    public UserDto getUserById(String userId) {
        return null;
    }

    @Override
    @Transactional
    public Iterable<UserDto> getAllUsers() {
        return userRepository
                .findAll().stream()
                .map(user -> modelMapper.map(user, UserDto.class))
                .toList();
    }
}
