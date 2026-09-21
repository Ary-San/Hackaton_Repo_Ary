package com.tuckersoft.branchengine.controller;

import com.tuckersoft.branchengine.dto.PathResponse;
import com.tuckersoft.branchengine.dto.PlaythroughRequest;
import com.tuckersoft.branchengine.dto.PlaythroughResponse;
import com.tuckersoft.branchengine.service.PlaythroughService;
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
@RequestMapping("/api/v1/playthroughs")
@RequiredArgsConstructor
public class PlaythroughController {

    private final PlaythroughService playthroughService;

    @PostMapping
    public ResponseEntity<PlaythroughResponse> abrir(@Valid @RequestBody PlaythroughRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(playthroughService.abrir(request));
    }

    /** Un ROLE_USER ve solo las suyas; el administrador supervisa todas. */
    @GetMapping
    public ResponseEntity<List<PlaythroughResponse>> listar() {
        return ResponseEntity.ok(playthroughService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlaythroughResponse> porId(@PathVariable Long id) {
        return ResponseEntity.ok(playthroughService.porId(id));
    }

    @GetMapping("/{id}/path")
    public ResponseEntity<PathResponse> recorrido(@PathVariable Long id) {
        return ResponseEntity.ok(playthroughService.recorrido(id));
    }
}
