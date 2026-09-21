package com.tuckersoft.branchengine.event;

import java.time.Instant;

/**
 * Lo que el listener necesita para escribir el Informe de Realidad.
 *
 * Va todo dentro del evento a proposito: el listener corre en otro hilo y despues del
 * commit, asi que alli ya no hay usuario autenticado ni sesion de JPA de la peticion.
 */
public record DecisionCommittedEvent(Long decisionId,
                                     String recipientEmail,
                                     String displayName,
                                     String playerTag,
                                     String branchType,
                                     String impactLevel,
                                     String handlerUnit,
                                     String outcomeCode,
                                     String sourceNodeCode,
                                     String resolvedNodeCode,
                                     String playthroughStatus,
                                     Integer lucidity,
                                     Integer controlLevel,
                                     String endingCode,
                                     String rawInput,
                                     Instant createdAt,
                                     boolean simularFalloDeCorreo) {
}
