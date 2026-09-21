package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.domain.Decision;
import com.tuckersoft.branchengine.domain.Playthrough;
import com.tuckersoft.branchengine.domain.PlaythroughStatus;
import com.tuckersoft.branchengine.domain.StoryNode;
import com.tuckersoft.branchengine.domain.User;
import com.tuckersoft.branchengine.dto.PathResponse;
import com.tuckersoft.branchengine.dto.PathStepResponse;
import com.tuckersoft.branchengine.dto.PlaythroughRequest;
import com.tuckersoft.branchengine.dto.PlaythroughResponse;
import com.tuckersoft.branchengine.exception.BadRequestException;
import com.tuckersoft.branchengine.exception.ConflictException;
import com.tuckersoft.branchengine.exception.ForbiddenException;
import com.tuckersoft.branchengine.exception.NotFoundException;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.PlaythroughRepository;
import com.tuckersoft.branchengine.repository.StoryNodeRepository;
import com.tuckersoft.branchengine.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaythroughService {

    private final PlaythroughRepository playthroughRepository;
    private final StoryNodeRepository storyNodeRepository;
    private final DecisionRepository decisionRepository;
    private final AuthenticatedUser authenticatedUser;

    @Transactional
    public PlaythroughResponse abrir(PlaythroughRequest request) {
        User dueno = authenticatedUser.actual();

        StoryNode nodo = storyNodeRepository.findByNodeCode(request.startNodeCode())
                .orElseThrow(() -> new NotFoundException(
                        "No existe la escena con codigo " + request.startNodeCode()));

        if (playthroughRepository.existsByPlayerTag(request.playerTag())) {
            throw new ConflictException("Ya existe una partida con la etiqueta " + request.playerTag());
        }

        if (nodo.estaLleno()) {
            throw new BadRequestException("La escena " + nodo.getNodeCode()
                    + " esta llena: " + nodo.getCurrentBranches() + "/" + nodo.getBranchCapacity());
        }

        Instant ahora = Instant.now();
        Playthrough partida = Playthrough.builder()
                .playerTag(request.playerTag())
                .user(dueno)
                .startNodeCode(nodo.getNodeCode())
                .currentNode(nodo)
                .lucidity(100)
                .controlLevel(0)
                .status(PlaythroughStatus.ACTIVA)
                .endingCode(null)
                .createdAt(ahora)
                .updatedAt(ahora)
                .build();

        nodo.setCurrentBranches(nodo.getCurrentBranches() + 1);
        storyNodeRepository.save(nodo);

        return PlaythroughResponse.de(playthroughRepository.save(partida));
    }

    /** El usuario normal ve solo las suyas; el administrador supervisa todas. */
    @Transactional(readOnly = true)
    public List<PlaythroughResponse> listar() {
        User actor = authenticatedUser.actual();

        List<Playthrough> partidas = AuthenticatedUser.esAdmin(actor)
                ? playthroughRepository.findAllByOrderByCreatedAtDesc()
                : playthroughRepository.findByUserIdOrderByCreatedAtDesc(actor.getId());

        return partidas.stream().map(PlaythroughResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public PlaythroughResponse porId(Long id) {
        return PlaythroughResponse.de(buscarConPermisoDeLectura(id));
    }

    @Transactional(readOnly = true)
    public PathResponse recorrido(Long id) {
        Playthrough partida = buscarConPermisoDeLectura(id);

        List<Decision> movimientos = decisionRepository
                .findByPlaythroughIdAndResolvedNodeCodeIsNotNullOrderByCreatedAtAscIdAsc(id);

        List<PathStepResponse> pasos = new ArrayList<>(movimientos.size());
        int orden = 1;
        for (Decision d : movimientos) {
            pasos.add(new PathStepResponse(orden++, d.getId(),
                    d.getNode().getNodeCode(), d.getResolvedNodeCode(),
                    d.getBranchType().name(), d.getImpactLevel().name(), d.getCreatedAt()));
        }

        return new PathResponse(partida.getId(), partida.getPlayerTag(),
                partida.getStatus().name(), partida.getEndingCode(),
                partida.getStartNodeCode(), partida.getCurrentNode().getNodeCode(), pasos);
    }

    /**
     * Leer una partida ajena solo se le permite al administrador. Escribir sobre ella
     * no se le permite a nadie, ni siquiera a el: eso se comprueba en DecisionService.
     */
    private Playthrough buscarConPermisoDeLectura(Long id) {
        Playthrough partida = playthroughRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe la partida con id " + id));

        User actor = authenticatedUser.actual();
        if (!AuthenticatedUser.esAdmin(actor) && !partida.getUser().getId().equals(actor.getId())) {
            throw new ForbiddenException("Esa partida no es tuya");
        }

        return partida;
    }
}
