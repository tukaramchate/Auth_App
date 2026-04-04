package com.validation.auth.backend.security;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.validation.auth.backend.config.RateLimitProperties;
import com.validation.auth.backend.dtos.ApiError;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final Map<String, Deque<Long>> requestWindows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    )
            throws ServletException, IOException {

        String requestMethod = nonNull(request.getMethod());

        if (!rateLimitProperties.isEnabled() || HttpMethod.OPTIONS.name().equalsIgnoreCase(requestMethod)) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<RateLimitProperties.Rule> matchedRule = rateLimitProperties.getRules().stream()
                .filter(rule -> matchesRule(rule, request))
                .findFirst();

        if (matchedRule.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitProperties.Rule rule = matchedRule.get();
        String key = buildKey(request, rule);

        if (isAllowed(key, rule)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(Math.max(1L, rule.getWindowSeconds())));

        ApiError apiError = new ApiError(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "Too Many Requests",
                "Rate limit exceeded. Please try again later.",
                request.getRequestURI(),
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        response.getWriter().write(objectMapper.writeValueAsString(apiError));
    }

    private boolean matchesRule(RateLimitProperties.Rule rule, HttpServletRequest request) {
        String rulePattern = nonNull(rule.getPathPattern());
        if (rulePattern.isBlank()) {
            return false;
        }

        String requestUri = nonNull(request.getRequestURI());
        boolean pathMatches = pathMatcher.match(rulePattern, requestUri);
        if (!pathMatches) {
            return false;
        }

        if (rule.getMethod() == null || rule.getMethod().isBlank()) {
            return true;
        }

        String requestMethod = nonNull(request.getMethod());
        return nonNull(rule.getMethod()).equalsIgnoreCase(requestMethod);
    }

    private String buildKey(HttpServletRequest request, RateLimitProperties.Rule rule) {
        String clientIp = extractClientIp(request);
        String ruleName = rule.getName() == null || rule.getName().isBlank() ? rule.getPathPattern() : rule.getName();
        return ruleName + ":" + clientIp;
    }

    private String extractClientIp(HttpServletRequest request) {
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

    private boolean isAllowed(String key, RateLimitProperties.Rule rule) {
        long now = System.currentTimeMillis();
        long windowMillis = Math.max(1L, rule.getWindowSeconds()) * 1000L;
        int maxRequests = Math.max(1, rule.getLimit());

        Deque<Long> timestamps = requestWindows.computeIfAbsent(key, ignored -> new ArrayDeque<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() >= windowMillis) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= maxRequests) {
                return false;
            }

            timestamps.addLast(now);
            return true;
        }
    }

    @NonNull
    private String nonNull(@Nullable String value) {
        return value == null ? "" : value;
    }
}
