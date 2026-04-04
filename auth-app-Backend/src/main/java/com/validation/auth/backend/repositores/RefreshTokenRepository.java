package com.validation.auth.backend.repositores;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.validation.auth.backend.entities.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByJti(String jti);

    Optional<RefreshToken> findByJtiAndUser_Id(String jti, UUID userId);

    List<RefreshToken> findAllByUser_IdOrderByCreatedAtDesc(UUID userId);

}

