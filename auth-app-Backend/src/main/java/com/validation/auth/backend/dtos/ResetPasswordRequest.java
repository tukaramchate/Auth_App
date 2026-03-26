package com.validation.auth.backend.dtos;

public record ResetPasswordRequest(
        String token,
        String newPassword
) {
}
