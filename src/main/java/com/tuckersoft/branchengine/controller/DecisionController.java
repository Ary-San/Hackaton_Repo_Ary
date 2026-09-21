package com.tuckersoft.branchengine.controller;

import com.tuckersoft.branchengine.dto.DecisionRequest;
import com.tuckersoft.branchengine.dto.DecisionResponse;
import com.tuckersoft.branchengine.dto.PageResponse;
import com.tuckersoft.branchengine.dto.RealityLogResponse;
import com.tuckersoft.branchengine.service.DecisionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/decisions")
@RequiredArgsConstructor
public class DecisionController {

    private final DecisionService decisionService;

    /**
     * Devuelve 201 de inmediato: el correo sale despues del commit y en otro hilo.
     *
     * X-Bandersnatch-Simulate es el modo QA. Es opcional y un valor desconocido no
     * cambia nada: nunca provoca un 400.
     */
    @PostMapping
    public ResponseEntity<DecisionResponse> registrar(
            @Valid @RequestBody DecisionRequest request,
            @RequestHeader(value = "X-Bandersnatch-Simulate", required = false) String simulate) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(decisionService.registrar(request, simulate));
    }

    @GetMapping
    public ResponseEntity<PageResponse<DecisionResponse>> listar(
            @RequestParam(required = false) String branchType,
            @RequestParam(required = false) String impactLevel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long playthroughId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(decisionService.listar(
                branchType, impactLevel, status, playthroughId, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DecisionResponse> porId(@PathVariable Long id) {
        return ResponseEntity.ok(decisionService.porId(id));
    }

    @GetMapping("/{id}/reality-logs")
    public ResponseEntity<List<RealityLogResponse>> informes(@PathVariable Long id) {
        return ResponseEntity.ok(decisionService.informes(id));
    }
}
