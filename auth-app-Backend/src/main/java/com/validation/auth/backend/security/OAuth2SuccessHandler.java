package com.validation.auth.backend.security;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.validation.auth.backend.entities.Provider;
import com.validation.auth.backend.entities.RefreshToken;
import com.validation.auth.backend.entities.User;
import com.validation.auth.backend.repositores.RefreshTokenRepository;
import com.validation.auth.backend.repositores.UserRepository;
import com.validation.auth.backend.services.DefaultRoleService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final CookieService cookieService;
    private final RefreshTokenRepository refreshTokenRepository;
        private final DefaultRoleService defaultRoleService;

    @Value("${app.auth.frontend.success-redirect}")
    private String frontEndSuccessUrl;


    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        logger.info("Successful authentication");


        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        //identify user:

        String registrationId = "unknown";
        if (authentication instanceof OAuth2AuthenticationToken token) {
            registrationId = token.getAuthorizedClientRegistrationId();
        }

        logger.info("OAuth2 registrationId: {}", registrationId);

        User user;
        switch (registrationId) {
            case "google" -> {
                String googleId = oAuth2User.getAttributes().getOrDefault("sub", "").toString();

                String email = oAuth2User.getAttributes().getOrDefault("email", "").toString();
                String name = oAuth2User.getAttributes().getOrDefault("name", "").toString();
                String picture = oAuth2User.getAttributes().getOrDefault("picture", "").toString();
                User newUser = User.builder()
                        .email(email)
                        .name(name)
                        .image(picture)
                        .enable(true)
                        .provider(Provider.GOOGLE)
                        .providerId(googleId)
                        .build();
                user = userRepository.findByEmail(email).orElseGet(() -> userRepository.save(newUser));

            }

            case "github" -> {
                String name = oAuth2User.getAttributes().getOrDefault("login", "").toString();
                String githubId = oAuth2User.getAttributes().getOrDefault("id", "").toString();
                String image = oAuth2User.getAttributes().getOrDefault("avatar_url", "").toString();

                String email = (String) oAuth2User.getAttributes().get("email");
                if (email == null) {
                    email = name + "@github.com";
                }

                User newUser = User.builder()
                        .email(email)
                        .name(name)
                        .image(image)
                        .enable(true)
                        .provider(Provider.GITHUB)
                        .providerId(githubId)
                        .build();
                user = userRepository.findByEmail(email).orElseGet(() -> userRepository.save(newUser));

            }

            case "linkedin" -> {

                String linkedinId = oAuth2User.getAttributes()
                        .getOrDefault("sub", "")
                        .toString();

                String email = oAuth2User.getAttributes()
                        .getOrDefault("email", "")
                        .toString();

                String name = oAuth2User.getAttributes()
                        .getOrDefault("name", "")
                        .toString();

                String picture = oAuth2User.getAttributes()
                        .getOrDefault("picture", "")
                        .toString();

                User newUser = User.builder()
                        .email(email)
                        .name(name)
                        .image(picture)
                        .enable(true)
                        .provider(Provider.LINKEDIN)
                        .providerId(linkedinId)
                        .build();

                user = userRepository
                        .findByEmail(email)
                        .orElseGet(() -> userRepository.save(newUser));
            }

            default -> {
                throw new RuntimeException("Invalid registration id");
            }

        }

        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            user.setRoles(defaultRoleService.ensureAtLeastUserRole(user.getRoles()));
            user = userRepository.save(user);
        }

        String jti = UUID.randomUUID().toString();
        RefreshToken refreshTokenOb = RefreshToken.builder()
                .jti(jti)
                .user(user)
                .revoked(false)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .build();

        refreshTokenRepository.save(refreshTokenOb);

        String refreshToken = jwtService.generateRefreshToken(user, refreshTokenOb.getJti());
        cookieService.attachRefreshCookie(response, refreshToken, (int) jwtService.getRefreshTtlSeconds());
//        response.getWriter().write("Login successful");
        response.sendRedirect(frontEndSuccessUrl);
    }
}