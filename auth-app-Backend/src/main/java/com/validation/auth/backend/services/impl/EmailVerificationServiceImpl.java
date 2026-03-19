package com.validation.auth.backend.services.impl;

import com.validation.auth.backend.entities.EmailVerificationToken;
import com.validation.auth.backend.entities.User;
import com.validation.auth.backend.repositores.EmailVerificationTokenRepository;
import com.validation.auth.backend.repositores.UserRepository;
import com.validation.auth.backend.services.EmailVerificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final UserRepository userRepository;
    private final JavaMailSender javaMailSender;

    @Value("${app.auth.verification.token-ttl-seconds:86400}")
    private long verificationTtlSeconds;

    @Value("${app.auth.frontend.verify-email-url:http://localhost:5173/verify-email}")
    private String verifyEmailUrl;

    @Value("${app.auth.email.from:no-reply@auth-app.local}")
    private String mailFrom;

    @Override
    @Transactional
    public void sendVerificationEmail(User user) {
        emailVerificationTokenRepository.deleteByUser_Id(user.getId());

        String token = UUID.randomUUID().toString();
        var verification = EmailVerificationToken.builder()
                .token(token)
                .user(user)
                .expiresAt(Instant.now().plusSeconds(verificationTtlSeconds))
                .build();

        emailVerificationTokenRepository.save(verification);

        String verifyLink = verifyEmailUrl + "?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(user.getEmail());
        message.setSubject("Verify your email");
        message.setText("Hi " + user.getName() + ",\n\n"
                + "Please verify your email by opening this link:\n"
                + verifyLink + "\n\n"
                + "This link expires in " + verificationTtlSeconds + " seconds.");
        javaMailSender.send(message);
    }

    @Override
    @Transactional
    public void verify(String token) {
        var verification = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadCredentialsException("Invalid verification token"));

        if (verification.getVerifiedAt() != null) {
            throw new BadCredentialsException("Email already verified");
        }

        if (verification.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Verification token expired");
        }

        User user = verification.getUser();
        user.setEnable(true);
        userRepository.save(user);

        verification.setVerifiedAt(Instant.now());
        emailVerificationTokenRepository.save(verification);
    }

    @Override
    @Transactional
    public void resendByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email"));

        if (user.isEnable()) {
            return;
        }

        sendVerificationEmail(user);
    }
}
