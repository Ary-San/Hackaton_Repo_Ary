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

/**
 * Una escena de Bandersnatch.
 *
 * primaryBranchCode y glitchBranchCode son Strings sueltos, no llaves foraneas: los
 * nodos se crean en cualquier orden y pueden apuntar a escenas que todavia no existen.
 * La resolucion ocurre al decidir, no al crear.
 */
@Entity
@Table(name = "story_nodes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String nodeCode;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sceneText;

    @Column(nullable = false)
    private Integer branchCapacity;

    @Column(nullable = false)
    private Integer currentBranches;

    private String primaryBranchCode;

    private String glitchBranchCode;

    @Column(nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "currentNode")
    @Builder.Default
    private List<Playthrough> playthroughs = new ArrayList<>();

    @OneToMany(mappedBy = "node")
    @Builder.Default
    private List<Decision> decisions = new ArrayList<>();

    /** Un nodo esta lleno cuando ya no admite mas partidas nuevas. */
    public boolean estaLleno() {
        return currentBranches >= branchCapacity;
    }
}
