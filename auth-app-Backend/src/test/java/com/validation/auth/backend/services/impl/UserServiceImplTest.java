package com.validation.auth.backend.services.impl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.validation.auth.backend.dtos.UserDto;
import com.validation.auth.backend.dtos.UserSelfUpdateRequest;
import com.validation.auth.backend.entities.User;
import com.validation.auth.backend.repositores.RoleRepository;
import com.validation.auth.backend.repositores.UserRepository;
import com.validation.auth.backend.services.DefaultRoleService;
import com.validation.auth.backend.exceptions.ResourceNotFoundException;
import com.validation.auth.backend.repositores.EmailVerificationTokenRepository;
import com.validation.auth.backend.repositores.PasswordResetTokenRepository;
import com.validation.auth.backend.repositores.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private DefaultRoleService defaultRoleService;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

        @Mock
        private RefreshTokenRepository refreshTokenRepository;

        @Mock
        private PasswordResetTokenRepository passwordResetTokenRepository;

        @Mock
        private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void getUsersPage_shouldClampAndNormalizeInputs() {
        User user = User.builder()
                .email("admin@example.com")
                .name("Admin")
                .build();
        UserDto dto = UserDto.builder()
                .email("admin@example.com")
                .name("Admin")
                .build();

        Page<User> repoPage = new PageImpl<>(List.of(user));
        when(userRepository.findUsersByFilters(any(), any(), any(), any(Pageable.class))).thenReturn(repoPage);
        when(modelMapper.map(user, UserDto.class)).thenReturn(dto);

        Page<UserDto> result = userService.getUsersPage("  admin  ", Boolean.TRUE, "  ROLE_ADMIN  ", -5, 999, "name,asc");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findUsersByFilters(eq("admin"), eq(Boolean.TRUE), eq("ROLE_ADMIN"), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(100);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "name"));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getEmail()).isEqualTo("admin@example.com");
    }

    @Test
    void getUsersPage_shouldUseDefaultSortAndNullFiltersForBlankInput() {
        User user = User.builder()
                .email("user@example.com")
                .name("User")
                .build();
        UserDto dto = UserDto.builder()
                .email("user@example.com")
                .name("User")
                .build();

        when(userRepository.findUsersByFilters(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));
        when(modelMapper.map(user, UserDto.class)).thenReturn(dto);

        userService.getUsersPage("   ", null, "   ", 2, 0, "  ");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findUsersByFilters(eq(null), eq(null), eq(null), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(1);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

        @Test
        void updateUser_shouldPreserveExistingEnableFlag() {
                User existingUser = User.builder()
                                .email("user@example.com")
                                .name("Old Name")
                                .enable(true)
                                .build();
                UserDto updateRequest = UserDto.builder()
                                .name("New Name")
                                .build();
                UserDto mappedResult = UserDto.builder()
                                .email("user@example.com")
                                .name("New Name")
                                .build();

                when(userRepository.findById(any())).thenReturn(java.util.Optional.of(existingUser));
                when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
                when(modelMapper.map(any(User.class), eq(UserDto.class))).thenReturn(mappedResult);

                UserDto result = userService.updateUser(updateRequest, "123e4567-e89b-12d3-a456-426614174000");

                ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
                verify(userRepository).save(userCaptor.capture());

                assertThat(userCaptor.getValue().isEnable()).isTrue();
                assertThat(userCaptor.getValue().getName()).isEqualTo("New Name");
                assertThat(result.getName()).isEqualTo("New Name");
        }

    @Test
    void updateCurrentUser_shouldUpdateOnlyEditableFields() {
        User existingUser = User.builder()
                .email("user@example.com")
                .name("Old Name")
                .image("https://example.com/old.png")
                .enable(true)
                .build();
        UserDto mapped = UserDto.builder()
                .email("user@example.com")
                .name("New Name")
                .image("https://example.com/new.png")
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(java.util.Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelMapper.map(any(User.class), eq(UserDto.class))).thenReturn(mapped);

        UserDto result = userService.updateCurrentUser(
                "user@example.com",
                UserSelfUpdateRequest.builder()
                        .name("  New Name  ")
                        .image("https://example.com/new.png")
                        .build()
        );

        ArgumentCaptor<User> savedCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedCaptor.capture());
        User saved = savedCaptor.getValue();

        assertThat(saved.getName()).isEqualTo("New Name");
        assertThat(saved.getImage()).isEqualTo("https://example.com/new.png");
        assertThat(saved.isEnable()).isTrue();
        assertThat(result.getName()).isEqualTo("New Name");
    }

    @Test
    void updateCurrentUser_shouldThrowWhenUserNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(java.util.Optional.empty());

        ResourceNotFoundException thrown = org.junit.jupiter.api.Assertions.assertThrows(
                ResourceNotFoundException.class,
                () -> userService.updateCurrentUser(
                        "missing@example.com",
                        UserSelfUpdateRequest.builder().name("Name").build()
                )
        );
        assertThat(thrown.getMessage()).isEqualTo("Current user not found");
    }

        @Test
        void deleteUser_shouldRemoveDependentTokensBeforeDeletingUser() {
                java.util.UUID userId = java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
                User existingUser = User.builder()
                                .id(userId)
                                .email("user@example.com")
                                .name("User")
                                .build();

                when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(existingUser));

                userService.deleteUser(userId.toString());

                verify(refreshTokenRepository).deleteByUser_Id(userId);
                verify(passwordResetTokenRepository).deleteByUser_Id(userId);
                verify(emailVerificationTokenRepository).deleteByUser_Id(userId);
                verify(userRepository).delete(existingUser);
        }
}
