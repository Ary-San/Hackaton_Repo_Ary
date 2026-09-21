package com.tuckersoft.branchengine.dto;

import com.tuckersoft.branchengine.domain.Decision;
import com.tuckersoft.branchengine.domain.Playthrough;

import java.time.Instant;

/**
 * playthroughStatus, lucidity, controlLevel y endingCode son el estado de la partida
 * DESPUES de aplicar la decision.
 */
public record DecisionResponse(Long id,
                               Long playthroughId,
                               String playerTag,
                               String sourceNodeCode,
                               String resolvedNodeCode,
                               String rawInput,
                               String branchType,
                               String impactLevel,
                               String handlerUnit,
                               String outcomeCode,
                               String status,
                               String playthroughStatus,
                               Integer lucidity,
                               Integer controlLevel,
                               String endingCode,
                               Instant createdAt,
                               Instant updatedAt) {

    public static DecisionResponse de(Decision d, Playthrough p) {
        return new DecisionResponse(
                d.getId(), p.getId(), p.getPlayerTag(),
                d.getNode().getNodeCode(), d.getResolvedNodeCode(), d.getRawInput(),
                d.getBranchType().name(), d.getImpactLevel().name(),
                d.getHandlerUnit(), d.getOutcomeCode(), d.getStatus().name(),
                p.getStatus().name(), p.getLucidity(), p.getControlLevel(), p.getEndingCode(),
                d.getCreatedAt(), d.getUpdatedAt());
    }

    public static DecisionResponse de(Decision d) {
        return de(d, d.getPlaythrough());
    }
}
