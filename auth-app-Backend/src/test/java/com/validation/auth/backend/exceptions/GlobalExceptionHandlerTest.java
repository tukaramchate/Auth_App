package com.validation.auth.backend.exceptions;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import com.validation.auth.backend.dtos.ApiErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleAuthException_shouldReturnUnifiedUnauthorizedPayload() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");

        ResponseEntity<ApiErrorResponse> response = handler.handleAuthException(
                new BadCredentialsException("Bad credentials"),
                request
        );

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("UNAUTHORIZED");
        assertThat(response.getBody().title()).isEqualTo("Authentication Failed");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/auth/login");
        assertThat(response.getBody().details()).isEmpty();
    }

    @Test
    void handleResourceNotFoundException_shouldReturnNotFoundWithResourceDetail() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/users/abc");

        ResponseEntity<ApiErrorResponse> response = handler.handleResourceNotFoundException(
                new ResourceNotFoundException("User not found with given id"),
                request
        );

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/users/abc");
        assertThat(response.getBody().details()).hasSize(1);
        assertThat(response.getBody().details().getFirst().field()).isEqualTo("resource");
        assertThat(response.getBody().details().getFirst().reason()).isEqualTo("User not found with given id");
    }

    @Test
    void handleUnhandledException_shouldReturnSafeInternalMessage() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/users");

        ResponseEntity<ApiErrorResponse> response = handler.handleUnhandledException(
                new RuntimeException("database exploded"),
                request
        );

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).isEqualTo("Something went wrong on our side. Please try again shortly.");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/users");
    }
}
