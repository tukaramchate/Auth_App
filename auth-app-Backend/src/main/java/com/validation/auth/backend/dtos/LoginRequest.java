package com.validation.auth.backend.dtos;

public record LoginRequest(
        String email,
        String password
) {

}