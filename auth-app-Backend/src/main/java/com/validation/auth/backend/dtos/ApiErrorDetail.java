package com.validation.auth.backend.dtos;

public record ApiErrorDetail(
        String field,
        String reason
) {
}
