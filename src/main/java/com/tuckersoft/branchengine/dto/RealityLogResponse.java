package com.tuckersoft.branchengine.dto;

import com.tuckersoft.branchengine.domain.RealityLog;

import java.time.Instant;

public record RealityLogResponse(Long id,
                                 Long decisionId,
                                 String recipientEmail,
                                 String subject,
                                 String logStatus,
                                 String errorMessage,
                                 Instant sentAt,
                                 Instant createdAt) {

    public static RealityLogResponse de(RealityLog log) {
        return new RealityLogResponse(log.getId(), log.getDecision().getId(),
                log.getRecipientEmail(), log.getSubject(), log.getLogStatus().name(),
                log.getErrorMessage(), log.getSentAt(), log.getCreatedAt());
    }
}
