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

/** Lo que el jugador decidio y hacia donde lo mando el motor. */
@Entity
@Table(name = "decisions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Decision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "playthrough_id", nullable = false)
    private Playthrough playthrough;

    /** Nodo de origen: el currentNode de la partida en el momento de decidir. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "node_id", nullable = false)
    private StoryNode node;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String rawInput;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BranchType branchType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImpactLevel impactLevel;

    @Column(nullable = false)
    private String handlerUnit;

    @Column(nullable = false)
    private String outcomeCode;

    /** Puede apuntar a un nodeCode que no corresponde a ningun StoryNode. */
    private String resolvedNodeCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DecisionStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "decision")
    @Builder.Default
    private List<RealityLog> realityLogs = new ArrayList<>();
}
