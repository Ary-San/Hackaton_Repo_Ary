package com.tuckersoft.branchengine.dto;

import com.tuckersoft.branchengine.domain.Playthrough;

import java.time.Instant;

public record PlaythroughResponse(Long id,
                                  String playerTag,
                                  String ownerEmail,
                                  String startNodeCode,
                                  String currentNodeCode,
                                  Integer lucidity,
                                  Integer controlLevel,
                                  String status,
                                  String endingCode,
                                  Instant createdAt,
                                  Instant updatedAt) {

    public static PlaythroughResponse de(Playthrough p) {
        return new PlaythroughResponse(p.getId(), p.getPlayerTag(), p.getUser().getEmail(),
                p.getStartNodeCode(), p.getCurrentNode().getNodeCode(),
                p.getLucidity(), p.getControlLevel(), p.getStatus().name(), p.getEndingCode(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
