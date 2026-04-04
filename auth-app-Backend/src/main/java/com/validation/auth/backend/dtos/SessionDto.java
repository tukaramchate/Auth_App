package com.validation.auth.backend.dtos;

import java.time.Instant;
import java.util.UUID;

public record SessionDto(
        UUID id,
        String jti,
        Instant createdAt,
        Instant expiresAt,
        Instant lastSeenAt,
        boolean revoked,
        String userAgent,
        String ipAddress,
        String deviceLabel
) {
}
