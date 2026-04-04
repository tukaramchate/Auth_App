package com.validation.auth.backend.dtos;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.validation.auth.backend.entities.Provider;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    private UUID id;
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    @NotBlank(message = "Name is required")
    private String name;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
    private String image;
    @Builder.Default
    private boolean enable = true;
    @Builder.Default
    private Instant createdAt =  Instant.now();
    @Builder.Default
    private Instant updatedAt =  Instant.now();
    @Builder.Default
    private Provider provider = Provider.LOCAL;
    @Builder.Default
    private Set<RoleDto> roles = new HashSet<>();

}
