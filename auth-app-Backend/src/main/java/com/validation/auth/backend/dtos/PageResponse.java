package com.validation.auth.backend.dtos;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;

public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious,
        String sort,
        Map<String, Object> filters
) {
    public static <T> PageResponse<T> from(Page<T> pageData, String sort, Map<String, Object> filters) {
        return new PageResponse<>(
                pageData.getContent(),
                pageData.getNumber(),
                pageData.getSize(),
                pageData.getTotalElements(),
                pageData.getTotalPages(),
                pageData.hasNext(),
                pageData.hasPrevious(),
                sort,
                filters
        );
    }
}
