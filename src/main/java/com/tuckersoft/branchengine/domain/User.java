package com.tuckersoft.branchengine.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** El analista de QA. Su email es el usuario de login y el destino del Informe de Realidad. */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    /** Siempre codificada con BCrypt. Nunca sale en un response. */
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 60)
    private String displayName;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private List<Playthrough> playthroughs = new ArrayList<>();
}
