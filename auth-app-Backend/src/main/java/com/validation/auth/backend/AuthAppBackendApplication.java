package com.validation.auth.backend;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.validation.auth.backend.config.AppConstants;
import com.validation.auth.backend.entities.Provider;
import com.validation.auth.backend.entities.Role;
import com.validation.auth.backend.entities.User;
import com.validation.auth.backend.repositores.RoleRepository;
import com.validation.auth.backend.repositores.UserRepository;

import lombok.RequiredArgsConstructor;

@SpringBootApplication
@RequiredArgsConstructor
public class AuthAppBackendApplication implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AuthAppBackendApplication.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.enabled:true}")
    private boolean adminBootstrapEnabled;

    @Value("${app.bootstrap.admin.first.email:admin1@auth-app.local}")
    private String firstAdminEmail;

    @Value("${app.bootstrap.admin.first.password:Admin@123}")
    private String firstAdminPassword;

    @Value("${app.bootstrap.admin.first.name:Primary Admin}")
    private String firstAdminName;

    @Value("${app.bootstrap.admin.second.email:admin2@auth-app.local}")
    private String secondAdminEmail;

    @Value("${app.bootstrap.admin.second.password:Admin@123}")
    private String secondAdminPassword;

    @Value("${app.bootstrap.admin.second.name:Secondary Admin}")
    private String secondAdminName;

	public static void main(String[] args) {
		SpringApplication.run(AuthAppBackendApplication.class, args);

	}

    @Override
    public void run(String... args) throws Exception {
        Role adminRole = ensureRole(AppConstants.ADMIN_ROLE);
        ensureRole(AppConstants.GUEST_ROLE);

        if (!adminBootstrapEnabled) {
            logger.info("Admin bootstrap is disabled");
            return;
        }

        createAdminIfMissing(firstAdminEmail, firstAdminPassword, firstAdminName, adminRole);
        createAdminIfMissing(secondAdminEmail, secondAdminPassword, secondAdminName, adminRole);
    }

    private Role ensureRole(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));
    }

    private void createAdminIfMissing(String email, String rawPassword, String name, Role adminRole) {
        if (email == null || email.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            logger.warn("Skipping admin bootstrap because email/password is blank");
            return;
        }

        if (userRepository.existsByEmail(email)) {
            logger.info("Admin user already exists: {}", email);
            return;
        }

        User admin = User.builder()
                .email(email)
                .name((name == null || name.isBlank()) ? "Admin" : name)
                .password(passwordEncoder.encode(rawPassword))
                .enable(true)
                .provider(Provider.LOCAL)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build();

        userRepository.save(admin);
        logger.info("Admin user created: {}", email);
    }



}
