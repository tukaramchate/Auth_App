package com.validation.auth.backend.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.validation.auth.backend.dtos.PageResponse;
import com.validation.auth.backend.dtos.SessionDto;
import com.validation.auth.backend.dtos.UserAdminUpdateRequest;
import com.validation.auth.backend.dtos.UserDto;
import com.validation.auth.backend.dtos.UserSelfUpdateRequest;
import com.validation.auth.backend.entities.User;
import com.validation.auth.backend.repositores.UserRepository;
import com.validation.auth.backend.services.SessionService;
import com.validation.auth.backend.services.UserService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@AllArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final SessionService sessionService;


    //create user api
        @PostMapping
        public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserDto userDto) {
            return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(userDto));
    }


    //get all user api
    @GetMapping
    public ResponseEntity<PageResponse<UserDto>> getAllUsers(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "enabled", required = false) Boolean enabled,
            @RequestParam(value = "role", required = false) String role
    ) {
        Page<UserDto> result = userService.getUsersPage(q, enabled, role, page, size, sort);
        Map<String, Object> filters = new HashMap<>();
        filters.put("q", q == null ? "" : q);
        filters.put("enabled", enabled);
        filters.put("role", role == null ? "" : role);
        return ResponseEntity.ok(PageResponse.from(result, sort, filters));
    }


    // get user by email
    @GetMapping("/email/{email}")
    public ResponseEntity<UserDto> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(userService.getUserByEmail(authentication.getName()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserDto> updateCurrentUser(
            Authentication authentication,
            @Valid @RequestBody UserSelfUpdateRequest request
    ) {
        return ResponseEntity.ok(userService.updateCurrentUser(authentication.getName(), request));
    }

    @GetMapping("/me/sessions")
    public ResponseEntity<List<SessionDto>> getCurrentUserSessions(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Current user not found"));
        return ResponseEntity.ok(sessionService.getSessionsForUser(user));
    }

    @DeleteMapping("/me/sessions")
    public ResponseEntity<Void> revokeAllCurrentUserSessions(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Current user not found"));
        sessionService.revokeAllSessionsForUser(user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me/sessions/{jti}")
    public ResponseEntity<Void> revokeCurrentUserSession(Authentication authentication, @PathVariable String jti) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Current user not found"));
        sessionService.revokeSessionForUser(user, jti);
        return ResponseEntity.noContent().build();
    }


    //delete user
    //api/v1/users/{userId}
    @DeleteMapping("/{userId}")
    public void deleteUser(@PathVariable("userId") String userId) {
        userService.deleteUser(userId);
    }


    //update user
    //api/v1/users/{userId}
    @PutMapping("/{userId}")
    public ResponseEntity<UserDto> updateUser(@Valid @RequestBody UserDto userDto, @PathVariable("userId") String userId) {
        return ResponseEntity.ok(userService.updateUser(userDto, userId));
    }

    @PutMapping("/{userId}/admin")
    public ResponseEntity<UserDto> updateUserAdmin(@Valid @RequestBody UserAdminUpdateRequest request, @PathVariable("userId") String userId) {
        UserDto userDto = UserDto.builder()
                .name(request.getName())
                .image(request.getImage())
                .enable(request.getEnable() == null || request.getEnable())
                .roles(request.getRoles())
                .build();
        return ResponseEntity.ok(userService.updateUserAdmin(userId, userDto));
    }

    @GetMapping("/roles")
    public ResponseEntity<List<String>> getSystemRoles() {
        return ResponseEntity.ok(userService.getSystemRoleNames());
    }

    @GetMapping("/{userId}/sessions")
    public ResponseEntity<List<SessionDto>> getUserSessions(@PathVariable String userId) {
        return ResponseEntity.ok(sessionService.getSessionsForUserId(java.util.UUID.fromString(userId)));
    }

    @DeleteMapping("/{userId}/sessions")
    public ResponseEntity<Void> revokeAllUserSessions(@PathVariable String userId) {
        sessionService.revokeAllSessionsForUserId(java.util.UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}/sessions/{jti}")
    public ResponseEntity<Void> revokeUserSession(@PathVariable String userId, @PathVariable String jti) {
        sessionService.revokeSessionForUserId(java.util.UUID.fromString(userId), jti);
        return ResponseEntity.noContent().build();
    }


    //get user by id
    //api/v1/users/{userId}
    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable("userId") String userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

}
