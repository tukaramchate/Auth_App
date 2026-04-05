package com.validation.auth.backend.services;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.validation.auth.backend.dtos.SessionDto;
import com.validation.auth.backend.entities.RefreshToken;
import com.validation.auth.backend.entities.User;

import jakarta.servlet.http.HttpServletRequest;

public interface SessionService {

    RefreshToken createSession(User user, String jti, HttpServletRequest request);

    RefreshToken createSession(User user, String jti, Instant expiresAt, HttpServletRequest request);

    List<SessionDto> getSessionsForUser(User user);

    List<SessionDto> getSessionsForUserId(UUID userId);

    void revokeSessionForUser(User user, String jti);

    void revokeSessionForUserId(UUID userId, String jti);

    void revokeAllSessionsForUser(User user);

    void revokeAllSessionsForUserId(UUID userId);

    void touchSession(String jti);
}
