package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.domain.StoryNode;
import com.tuckersoft.branchengine.dto.StoryNodeRequest;
import com.tuckersoft.branchengine.dto.StoryNodeResponse;
import com.tuckersoft.branchengine.exception.ConflictException;
import com.tuckersoft.branchengine.exception.NotFoundException;
import com.tuckersoft.branchengine.repository.StoryNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoryNodeService {

    private final StoryNodeRepository storyNodeRepository;

    @Transactional
    public StoryNodeResponse crear(StoryNodeRequest request) {
        if (storyNodeRepository.existsByNodeCode(request.nodeCode())) {
            throw new ConflictException("Ya existe una escena con el codigo " + request.nodeCode());
        }

        StoryNode nodo = StoryNode.builder()
                .nodeCode(request.nodeCode())
                .title(request.title())
                .sceneText(request.sceneText())
                .branchCapacity(request.branchCapacity())
                .currentBranches(0)
                .primaryBranchCode(request.primaryBranchCode())
                .glitchBranchCode(request.glitchBranchCode())
                .createdAt(Instant.now())
                .build();

        return StoryNodeResponse.de(storyNodeRepository.save(nodo));
    }

    @Transactional(readOnly = true)
    public List<StoryNodeResponse> listar() {
        return storyNodeRepository.findAllByOrderByIdAsc().stream()
                .map(StoryNodeResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public StoryNodeResponse porId(Long id) {
        return StoryNodeResponse.de(storyNodeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe la escena con id " + id)));
    }
}
