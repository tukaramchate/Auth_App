package com.validation.auth.backend.services.impl;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.validation.auth.backend.dtos.UserDto;
import com.validation.auth.backend.entities.PasswordResetToken;
import com.validation.auth.backend.entities.Provider;
import com.validation.auth.backend.entities.User;
import com.validation.auth.backend.repositores.PasswordResetTokenRepository;
import com.validation.auth.backend.repositores.UserRepository;
import com.validation.auth.backend.services.AuthService;
import com.validation.auth.backend.services.EmailVerificationService;
import com.validation.auth.backend.services.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender javaMailSender;
    private final EmailVerificationService emailVerificationService;

    @Value("${app.auth.verification.enabled:true}")
    private boolean emailVerificationEnabled;

    @Value("${app.auth.password-reset.enabled:true}")
    private boolean passwordResetEnabled;

    @Value("${app.auth.password-reset.token-ttl-seconds:1800}")
    private long passwordResetTokenTtlSeconds;

    @Value("${app.auth.frontend.reset-password-url:http://localhost:5173/reset-password}")
    private String resetPasswordUrl;

    @Value("${app.auth.email.from:no-reply@auth-app.local}")
    private String mailFrom;

    @Override
    public UserDto registerUser(UserDto userDto) {
        userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));
        userDto.setProvider(Provider.LOCAL);
        userDto.setEnable(!emailVerificationEnabled);

        UserDto createdUser = userService.createUser(userDto);
        if (emailVerificationEnabled) {
            emailVerificationService.resendByEmail(createdUser.getEmail());
        }
        return createdUser;
    }

    @Override
    public void verifyEmail(String token) {
        if (!emailVerificationEnabled) {
            return;
        }
        emailVerificationService.verify(token);
    }

    @Override
    public void resendVerificationEmail(String email) {
        if (!emailVerificationEnabled) {
            return;
        }
        emailVerificationService.resendByEmail(email);
    }

    @Override
    public void forgotPassword(String email) {
        if (!passwordResetEnabled) {
            return;
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return;
        }

        passwordResetTokenRepository.deleteByUser_Id(user.getId());

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiresAt(Instant.now().plusSeconds(passwordResetTokenTtlSeconds))
                .build();
        passwordResetTokenRepository.save(resetToken);

        String resetLink = resetPasswordUrl + "?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(user.getEmail());
        message.setSubject("Reset your password");
        message.setText("Hi " + user.getName() + ",\n\n"
                + "We received a request to reset your password.\n"
                + "Use this link to continue:\n"
                + resetLink + "\n\n"
                + "This link expires in " + passwordResetTokenTtlSeconds + " seconds.");
        javaMailSender.send(message);
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        if (!passwordResetEnabled) {
            return;
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadCredentialsException("Invalid reset token"));

        if (resetToken.getUsedAt() != null) {
            throw new BadCredentialsException("Reset token already used");
        }

        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Reset token expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(resetToken);
    }

}
