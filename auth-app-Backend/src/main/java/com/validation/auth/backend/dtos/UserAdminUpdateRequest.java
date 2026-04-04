package com.validation.auth.backend.dtos;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.NotEmpty;
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
public class UserAdminUpdateRequest {

    private String name;

    private String image;

    private Boolean enable;

    @NotEmpty(message = "At least one role is required")
    @Builder.Default
    private Set<RoleDto> roles = new HashSet<>();
}