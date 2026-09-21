package com.tuckersoft.branchengine.dto;

import com.tuckersoft.branchengine.domain.StoryNode;

import java.time.Instant;

public record StoryNodeResponse(Long id,
                                String nodeCode,
                                String title,
                                String sceneText,
                                Integer branchCapacity,
                                Integer currentBranches,
                                String primaryBranchCode,
                                String glitchBranchCode,
                                Instant createdAt) {

    public static StoryNodeResponse de(StoryNode nodo) {
        return new StoryNodeResponse(nodo.getId(), nodo.getNodeCode(), nodo.getTitle(),
                nodo.getSceneText(), nodo.getBranchCapacity(), nodo.getCurrentBranches(),
                nodo.getPrimaryBranchCode(), nodo.getGlitchBranchCode(), nodo.getCreatedAt());
    }
}
