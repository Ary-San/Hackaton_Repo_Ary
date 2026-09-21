package com.tuckersoft.branchengine.controller;

import com.tuckersoft.branchengine.dto.StoryNodeRequest;
import com.tuckersoft.branchengine.dto.StoryNodeResponse;
import com.tuckersoft.branchengine.service.StoryNodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/nodes")
@RequiredArgsConstructor
public class StoryNodeController {

    private final StoryNodeService storyNodeService;

    /** Solo ROLE_ADMIN: la regla vive en el SecurityFilterChain. */
    @PostMapping
    public ResponseEntity<StoryNodeResponse> crear(@Valid @RequestBody StoryNodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storyNodeService.crear(request));
    }

    @GetMapping
    public ResponseEntity<List<StoryNodeResponse>> listar() {
        return ResponseEntity.ok(storyNodeService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoryNodeResponse> porId(@PathVariable Long id) {
        return ResponseEntity.ok(storyNodeService.porId(id));
    }
}
