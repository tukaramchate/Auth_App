package com.validation.auth.backend.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "roles_id", updatable = false, nullable = false)
    private UUID id;


    @Column(unique = true, nullable = false, updatable = false)
    private String name;

}
