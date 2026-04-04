package com.validation.auth.backend.config;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.validation.auth.backend.dtos.ApiError;
import com.validation.auth.backend.security.JwtAuthenticationFilter;
import com.validation.auth.backend.security.RateLimitingFilter;

@Configuration
public class  SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final AuthenticationSuccessHandler successHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RateLimitingFilter rateLimitingFilter,
            AuthenticationSuccessHandler successHandler
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitingFilter = rateLimitingFilter;
        this.successHandler = successHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${app.auth.frontend.failure-redirect}") String frontEndFailureUrl
    ) throws Exception {

        http.csrf(AbstractHttpConfigurer :: disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                authorizeRequests ->
                        authorizeRequests
                    .requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated()
                    .requestMatchers("/api/v1/users/**").hasAuthority(AppConstants.ADMIN_ROLE)
                                .requestMatchers(AppConstants.AUTH_PUBLIC_URLS).permitAll()
                                .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 ->
                            oauth2.successHandler(successHandler)
                            .failureHandler((request, response, exception) ->
                                response.sendRedirect(frontEndFailureUrl))
                        )
                .logout(AbstractHttpConfigurer :: disable)
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint((request, response, e) -> {
                    logger.debug("Unauthorized access attempt: {}", e.getMessage());
                    response.setStatus(401);
                    response.setContentType("application/json");
                    String message = e.getMessage();

                    String error = (String) request.getAttribute("error");
                    if (error != null) {
                        message = error;
                    }
                    var apiError = ApiError.of(HttpStatus.UNAUTHORIZED.value(), "Unauthorized Access", message, request.getRequestURI(), true);
                    var objectMapper = new ObjectMapper();
                    response.getWriter().write(objectMapper.writeValueAsString(apiError));
                }))
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();

    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

//    @Bean
//    public UserDetailsService users() {
//        User.UserBuilder userBuilder = User.withDefaultPasswordEncoder();
//
//        UserDetails user1 = userBuilder
//                .username("tukaram")
//                .password("abc")
//                .roles("ADMIN")
//                .build();
//
//        UserDetails user2 = userBuilder
//                .username("ganesh")
//                .password("abc")
//                .roles("ADMIN")
//                .build();
//
//        UserDetails user3 = userBuilder
//                .username("ram")
//                .password("abc")
//                .roles("USER")
//                .build();
//
//        return new InMemoryUserDetailsManager(user1, user2, user3);
//    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.front-end-url}") String corsUrls
    ) {

        String[] urls = corsUrls.trim().split(",");

        var config= new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(urls));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS","PATCH", "HEAD"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        var source= new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;


    }

}
