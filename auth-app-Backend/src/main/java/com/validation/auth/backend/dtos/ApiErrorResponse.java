package com.validation.auth.backend.dtos;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

public record ApiErrorResponse(
        int status,
        String code,
        String title,
        String message,
        String path,
        OffsetDateTime timestamp,
        String traceId,
        List<ApiErrorDetail> details
) {
    public static ApiErrorResponse of(
            int status,
            String code,
            String title,
            String message,
            String path,
            List<ApiErrorDetail> details
    ) {
        return new ApiErrorResponse(
                status,
                code,
                title,
                message,
                path,
                OffsetDateTime.now(ZoneOffset.UTC),
                UUID.randomUUID().toString(),
                details == null ? List.of() : details
        );
    }
}
