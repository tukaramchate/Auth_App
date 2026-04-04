package com.validation.auth.backend.services.impl;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.validation.auth.backend.dtos.SessionDto;
import com.validation.auth.backend.entities.RefreshToken;
import com.validation.auth.backend.entities.User;
import com.validation.auth.backend.repositores.RefreshTokenRepository;
import com.validation.auth.backend.repositores.UserRepository;
import com.validation.auth.backend.services.SessionService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SessionServiceImpl implements SessionService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Override
    public RefreshToken createSession(User user, String jti, HttpServletRequest request) {
        return createSession(user, jti, Instant.now(), request);
    }

    @Override
    public RefreshToken createSession(User user, String jti, Instant expiresAt, HttpServletRequest request) {
        RefreshToken refreshToken = RefreshToken.builder()
                .jti(jti)
                .user(user)
                .createdAt(Instant.now())
                .lastSeenAt(Instant.now())
                .expiresAt(expiresAt)
                .revoked(false)
                .userAgent(extractUserAgent(request))
                .ipAddress(extractClientIp(request))
                .build();
            return refreshTokenRepository.save(Objects.requireNonNull(refreshToken));
    }

    @Override
    public List<SessionDto> getSessionsForUser(User user) {
        return refreshTokenRepository.findAllByUser_IdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<SessionDto> getSessionsForUserId(UUID userId) {
        User user = userRepository.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        return getSessionsForUser(user);
    }

    @Override
    public void revokeSessionForUser(User user, String jti) {
        revokeSessionForUserId(user.getId(), jti);
    }

    @Override
    public void revokeSessionForUserId(UUID userId, String jti) {
        RefreshToken refreshToken = refreshTokenRepository.findByJtiAndUser_Id(jti, userId)
                .orElseThrow(() -> new BadCredentialsException("Session not found"));
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    @Override
    public void revokeAllSessionsForUser(User user) {
        revokeAllSessionsForUserId(user.getId());
    }

    @Override
    public void revokeAllSessionsForUserId(UUID userId) {
        refreshTokenRepository.findAllByUser_IdOrderByCreatedAtDesc(userId)
                .forEach(refreshToken -> {
                    refreshToken.setRevoked(true);
                    refreshTokenRepository.save(refreshToken);
                });
    }

    @Override
    public void touchSession(String jti) {
        refreshTokenRepository.findByJti(jti).ifPresent(refreshToken -> {
            refreshToken.setLastSeenAt(Instant.now());
            refreshTokenRepository.save(refreshToken);
        });
    }

    private SessionDto toDto(RefreshToken refreshToken) {
        return new SessionDto(
                refreshToken.getId(),
                refreshToken.getJti(),
                refreshToken.getCreatedAt(),
                refreshToken.getExpiresAt(),
                refreshToken.getLastSeenAt(),
                refreshToken.isRevoked(),
                refreshToken.getUserAgent(),
                refreshToken.getIpAddress(),
                buildDeviceLabel(refreshToken.getUserAgent())
        );
    }

    private String extractUserAgent(HttpServletRequest request) {
        return request == null ? null : request.getHeader("User-Agent");
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            int commaIndex = forwardedFor.indexOf(',');
            return (commaIndex > 0 ? forwardedFor.substring(0, commaIndex) : forwardedFor).trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private String buildDeviceLabel(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown device";
        }

        String normalized = userAgent.toLowerCase(Locale.ROOT);
        if (normalized.contains("iphone") || normalized.contains("android")) {
            return "Mobile device";
        }
        if (normalized.contains("windows")) {
            return "Windows browser";
        }
        if (normalized.contains("mac os") || normalized.contains("macintosh")) {
            return "Mac browser";
        }
        if (normalized.contains("linux")) {
            return "Linux browser";
        }
        return "Browser session";
    }
}
