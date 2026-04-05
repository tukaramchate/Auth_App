package com.validation.auth.backend.repositores;

import com.validation.auth.backend.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

        @Query("""
            SELECT DISTINCT u
            FROM User u
            LEFT JOIN u.roles r
            WHERE (:q IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:enabled IS NULL OR u.enable = :enabled)
              AND (:role IS NULL OR r.name = :role)
            """)
        Page<User> findUsersByFilters(
            @Param("q") String q,
            @Param("enabled") Boolean enabled,
            @Param("role") String role,
            Pageable pageable
        );

}
