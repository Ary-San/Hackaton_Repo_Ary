package com.tuckersoft.branchengine.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Audita cada intento de envio del Informe de Realidad, salga bien o mal. */
@Entity
@Table(name = "reality_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RealityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "decision_id", nullable = false)
    private Decision decision;

    @Column(nullable = false)
    private String recipientEmail;

    @Column(nullable = false)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LogStatus logStatus;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /** Solo se rellena cuando el envio sale bien. */
    private Instant sentAt;

    @Column(nullable = false)
    private Instant createdAt;
}
