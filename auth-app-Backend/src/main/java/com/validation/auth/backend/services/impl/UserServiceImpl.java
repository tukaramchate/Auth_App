package com.validation.auth.backend.services.impl;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.validation.auth.backend.dtos.UserDto;
import com.validation.auth.backend.dtos.UserSelfUpdateRequest;
import com.validation.auth.backend.entities.Provider;
import com.validation.auth.backend.entities.Role;
import com.validation.auth.backend.entities.User;
import com.validation.auth.backend.exceptions.ResourceNotFoundException;
import com.validation.auth.backend.helpers.UserHelper;
import com.validation.auth.backend.repositores.EmailVerificationTokenRepository;
import com.validation.auth.backend.repositores.PasswordResetTokenRepository;
import com.validation.auth.backend.repositores.RefreshTokenRepository;
import com.validation.auth.backend.repositores.RoleRepository;
import com.validation.auth.backend.repositores.UserRepository;
import com.validation.auth.backend.services.DefaultRoleService;
import com.validation.auth.backend.services.UserService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final DefaultRoleService defaultRoleService;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;

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
        if (user.getPassword() != null && !user.getPassword().isBlank() && !isBcryptHash(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        user.setRoles(defaultRoleService.ensureAtLeastUserRole(user.getRoles()));

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
        UUID uId = Objects.requireNonNull(UserHelper.parseUUID(userId));
        User existingUser = userRepository
                .findById(uId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with given id"));
        //we are not going to change email id for this project.
        if (userDto.getName() != null) existingUser.setName(userDto.getName());
        if (userDto.getImage() != null) existingUser.setImage(userDto.getImage());
        if (userDto.getProvider() != null) existingUser.setProvider(userDto.getProvider());
        if (userDto.getPassword() != null && !userDto.getPassword().isBlank()) {
            existingUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
        }
        if (userDto.getRoles() != null && !userDto.getRoles().isEmpty()) {
            existingUser.setRoles(resolveRoles(userDto.getRoles().stream().map(roleDto -> roleDto.getName()).filter(name -> name != null && !name.isBlank()).toList()));
        }
        existingUser.setUpdatedAt(Instant.now());
        User updatedUser = userRepository.save(existingUser);
        return modelMapper.map(updatedUser, UserDto.class);
    }

    @Override
    @Transactional
    public UserDto updateCurrentUser(String email, UserSelfUpdateRequest request) {
        User existingUser = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        existingUser.setName(request.getName().trim());
        existingUser.setImage(request.getImage() == null ? null : request.getImage().trim());
        existingUser.setUpdatedAt(Instant.now());

        User updatedUser = userRepository.save(existingUser);
        return modelMapper.map(updatedUser, UserDto.class);
    }

    @Override
    public void deleteUser(String userId) {
        UUID uId = Objects.requireNonNull(UserHelper.parseUUID(userId));
        User user = Objects.requireNonNull(userRepository.findById(uId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with given id")));

        // Delete dependent token records first to satisfy FK constraints.
        refreshTokenRepository.deleteByUser_Id(user.getId());
        passwordResetTokenRepository.deleteByUser_Id(user.getId());
        emailVerificationTokenRepository.deleteByUser_Id(user.getId());

        // Clear role links explicitly so user_roles join rows are removed before user delete.
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            user.getRoles().clear();
            userRepository.save(user);
            userRepository.flush();
        }

        userRepository.delete(user);
    }

    @Override
    public UserDto getUserById(String userId) {
        User user = userRepository.findById(Objects.requireNonNull(UserHelper.parseUUID(userId))).orElseThrow(() -> new ResourceNotFoundException("User not found with given id"));
        return modelMapper.map(user, UserDto.class);
    }

    @Override
    @Transactional
    public Iterable<UserDto> getAllUsers() {
        return userRepository
                .findAll().stream()
                .map(user -> modelMapper.map(user, UserDto.class))
                .toList();
    }

    @Override
    @Transactional
    public Page<UserDto> getUsersPage(String q, Boolean enabled, String role, int page, int size, String sort) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        Sort parsedSort = parseSort(sort);
        Pageable pageable = PageRequest.of(safePage, safeSize, parsedSort);

        String normalizedQ = normalize(q);
        String normalizedRole = normalize(role);

        Page<User> userPage = userRepository.findUsersByFilters(normalizedQ, enabled, normalizedRole, pageable);
        List<UserDto> userDtos = userPage.getContent().stream()
                .map(user -> modelMapper.map(user, UserDto.class))
                .toList();

        return new PageImpl<>(userDtos, pageable, userPage.getTotalElements());
    }

    @Override
    @Transactional
    public UserDto updateUserAdmin(String userId, UserDto userDto) {
        UUID uId = Objects.requireNonNull(UserHelper.parseUUID(userId));
        User existingUser = userRepository.findById(uId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with given id"));

        if (userDto.getName() != null) {
            existingUser.setName(userDto.getName());
        }
        if (userDto.getImage() != null) {
            existingUser.setImage(userDto.getImage());
        }
        if (userDto.getProvider() != null) {
            existingUser.setProvider(userDto.getProvider());
        }
        existingUser.setEnable(userDto.isEnable());
        if (userDto.getRoles() != null && !userDto.getRoles().isEmpty()) {
            existingUser.setRoles(resolveRoles(userDto.getRoles().stream().map(roleDto -> roleDto.getName()).filter(name -> name != null && !name.isBlank()).toList()));
        }
        if (userDto.getPassword() != null && !userDto.getPassword().isBlank()) {
            existingUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
        }
        existingUser.setUpdatedAt(Instant.now());
        return modelMapper.map(userRepository.save(existingUser), UserDto.class);
    }

    @Override
    public List<String> getSystemRoleNames() {
        return roleRepository.findAll().stream()
                .map(Role::getName)
                .toList();
    }

    private boolean isBcryptHash(String value) {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }

    private java.util.Set<Role> resolveRoles(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return defaultRoleService.ensureAtLeastUserRole(null);
        }

        return roleNames.stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid role: " + roleName)))
                .collect(java.util.stream.Collectors.toSet());
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        String[] parts = sort.split(",");
        String field = parts[0].trim();
        if (field.isBlank()) {
            field = "createdAt";
        }
        Sort.Direction direction = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim()))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, field);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

}
