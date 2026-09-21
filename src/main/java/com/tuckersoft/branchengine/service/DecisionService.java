package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.domain.BranchType;
import com.tuckersoft.branchengine.domain.Decision;
import com.tuckersoft.branchengine.domain.DecisionStatus;
import com.tuckersoft.branchengine.domain.ImpactLevel;
import com.tuckersoft.branchengine.domain.Playthrough;
import com.tuckersoft.branchengine.domain.PlaythroughStatus;
import com.tuckersoft.branchengine.domain.StoryNode;
import com.tuckersoft.branchengine.domain.User;
import com.tuckersoft.branchengine.dto.DecisionRequest;
import com.tuckersoft.branchengine.dto.DecisionResponse;
import com.tuckersoft.branchengine.dto.PageResponse;
import com.tuckersoft.branchengine.dto.RealityLogResponse;
import com.tuckersoft.branchengine.event.DecisionCommittedEvent;
import com.tuckersoft.branchengine.exception.ConflictException;
import com.tuckersoft.branchengine.exception.ForbiddenException;
import com.tuckersoft.branchengine.exception.NotFoundException;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.PlaythroughRepository;
import com.tuckersoft.branchengine.repository.RealityLogRepository;
import com.tuckersoft.branchengine.repository.StoryNodeRepository;
import com.tuckersoft.branchengine.security.AuthenticatedUser;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * El motor de ramas.
 *
 * Este service no conoce el correo: publica un DecisionCommittedEvent y se olvida.
 * Quien envia el Informe de Realidad es BranchNotificationListener, en otro bean y
 * en otro hilo, despues del commit.
 */
@Service
@RequiredArgsConstructor
public class DecisionService {

    public static final String ENDING_PAC_SYMBOL = "ENDING_PAC_SYMBOL";
    public static final String ENDING_WHITE_BEAR = "ENDING_WHITE_BEAR";
    public static final String ENDING_NETFLIX_CUT = "ENDING_NETFLIX_CUT";

    /** Valor de la cabecera de modo QA que fuerza el fallo de SMTP. */
    public static final String SIMULAR_FALLO_CORREO = "MAIL_FAILURE";

    private final DecisionRepository decisionRepository;
    private final PlaythroughRepository playthroughRepository;
    private final StoryNodeRepository storyNodeRepository;
    private final RealityLogRepository realityLogRepository;
    private final AuthenticatedUser authenticatedUser;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public DecisionResponse registrar(DecisionRequest request, String cabeceraSimulate) {
        // 1. El usuario del token, y la partida tiene que ser suya.
        User actor = authenticatedUser.actual();

        Playthrough partida = playthroughRepository.findById(request.playthroughId())
                .orElseThrow(() -> new NotFoundException(
                        "No existe la partida con id " + request.playthroughId()));

        // Supervisar no es jugar: ni siquiera el administrador decide sobre una partida ajena.
        if (!partida.getUser().getId().equals(actor.getId())) {
            throw new ForbiddenException("Esa partida no es tuya: no puedes decidir sobre ella");
        }

        // 2. Una partida que ya termino no acepta mas decisiones.
        if (partida.getStatus() == PlaythroughStatus.FINALIZADA) {
            throw new ConflictException("La partida " + partida.getPlayerTag()
                    + " ya esta FINALIZADA");
        }

        ImpactLevel impacto = ImpactLevel.valueOf(request.impactLevel());
        StoryNode origen = partida.getCurrentNode();
        Instant ahora = Instant.now();

        // 3. Clasificar, y derivar departamento y consecuencia de la rama.
        BranchType rama = BranchClassifier.clasificar(request.rawInput());

        Decision decision = Decision.builder()
                .playthrough(partida)
                .node(origen)
                .rawInput(request.rawInput())
                .branchType(rama)
                .impactLevel(impacto)
                .handlerUnit(rama.getHandlerUnit())
                .outcomeCode(rama.getOutcomeCode())
                .createdAt(ahora)
                .updatedAt(ahora)
                .build();

        // 4. La entrada corrupta se guarda y se descarta: no toca la partida ni publica evento.
        if (rama == BranchType.ENTRADA_CORRUPTA) {
            decision.setResolvedNodeCode(null);
            decision.setStatus(DecisionStatus.ERROR);
            decisionRepository.save(decision);
            return DecisionResponse.de(decision, partida);
        }

        // 5a. Stats, con los limites estrictos de 0 a 100.
        partida.setLucidity(acotar(partida.getLucidity() + impacto.getLucidityDelta()));
        partida.setControlLevel(acotar(partida.getControlLevel() + impacto.getControlDelta()));

        // 5b. Nodo destino: la rama glitch si rompio la cuarta pared o si el impacto es CRITICO.
        boolean porGlitch = rama == BranchType.RUPTURA_CUARTA_PARED || impacto == ImpactLevel.CRITICO;
        String codigoDestino = porGlitch ? origen.getGlitchBranchCode() : origen.getPrimaryBranchCode();
        decision.setResolvedNodeCode(codigoDestino);

        StoryNode destino = codigoDestino == null
                ? null
                : storyNodeRepository.findByNodeCode(codigoDestino).orElse(null);

        // 5c. Estado de la partida, en este orden exacto.
        if (partida.getControlLevel() >= 100) {
            terminar(partida, ENDING_PAC_SYMBOL);
        } else if (partida.getLucidity() <= 0) {
            terminar(partida, ENDING_WHITE_BEAR);
        } else if (destino == null) {
            terminar(partida, ENDING_NETFLIX_CUT);
        } else {
            partida.setCurrentNode(destino);
        }
        partida.setUpdatedAt(ahora);

        // 6 y 7. Se guarda la partida y la decision, que nace REGISTRADA.
        playthroughRepository.save(partida);
        decision.setStatus(DecisionStatus.REGISTRADA);
        decisionRepository.save(decision);

        // 8. El evento se procesa despues del COMMIT y en otro hilo.
        eventPublisher.publishEvent(new DecisionCommittedEvent(
                decision.getId(),
                partida.getUser().getEmail(),
                partida.getUser().getDisplayName(),
                partida.getPlayerTag(),
                rama.name(),
                impacto.name(),
                decision.getHandlerUnit(),
                decision.getOutcomeCode(),
                origen.getNodeCode(),
                decision.getResolvedNodeCode(),
                partida.getStatus().name(),
                partida.getLucidity(),
                partida.getControlLevel(),
                partida.getEndingCode(),
                decision.getRawInput(),
                decision.getCreatedAt(),
                SIMULAR_FALLO_CORREO.equalsIgnoreCase(cabeceraSimulate)));

        // 9.
        return DecisionResponse.de(decision, partida);
    }

    @Transactional(readOnly = true)
    public DecisionResponse porId(Long id) {
        return DecisionResponse.de(buscarConPermisoDeLectura(id));
    }

    @Transactional(readOnly = true)
    public List<RealityLogResponse> informes(Long decisionId) {
        buscarConPermisoDeLectura(decisionId);
        return realityLogRepository.findByDecisionIdOrderByCreatedAtAscIdAsc(decisionId).stream()
                .map(RealityLogResponse::de)
                .toList();
    }

    /**
     * Listado paginado. El filtrado ocurre en el repositorio: un ROLE_USER nunca llega
     * a traerse de la base las decisiones de otro.
     */
    @Transactional(readOnly = true)
    public PageResponse<DecisionResponse> listar(String branchType, String impactLevel,
                                                 String status, Long playthroughId,
                                                 int page, int size) {
        User actor = authenticatedUser.actual();
        Long duenoId = AuthenticatedUser.esAdmin(actor) ? null : actor.getId();

        BranchType rama = parsear(BranchType.class, branchType);
        ImpactLevel impacto = parsear(ImpactLevel.class, impactLevel);
        DecisionStatus estado = parsear(DecisionStatus.class, status);

        // Un filtro con un valor que no existe en el enum no puede casar con nada.
        if (noParseado(branchType, rama) || noParseado(impactLevel, impacto)
                || noParseado(status, estado)) {
            return new PageResponse<>(List.of(), 0, 0, page, size);
        }

        Specification<Decision> filtro = (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (duenoId != null) {
                condiciones.add(cb.equal(raiz.get("playthrough").get("user").get("id"), duenoId));
            }
            if (rama != null) condiciones.add(cb.equal(raiz.get("branchType"), rama));
            if (impacto != null) condiciones.add(cb.equal(raiz.get("impactLevel"), impacto));
            if (estado != null) condiciones.add(cb.equal(raiz.get("status"), estado));
            if (playthroughId != null) {
                condiciones.add(cb.equal(raiz.get("playthrough").get("id"), playthroughId));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };

        Page<Decision> pagina = decisionRepository.findAll(filtro,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));

        return PageResponse.of(pagina, DecisionResponse::de);
    }

    private Decision buscarConPermisoDeLectura(Long id) {
        Decision decision = decisionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe la decision con id " + id));

        User actor = authenticatedUser.actual();
        boolean esDueno = decision.getPlaythrough().getUser().getId().equals(actor.getId());
        if (!AuthenticatedUser.esAdmin(actor) && !esDueno) {
            throw new ForbiddenException("Esa decision pertenece a una partida que no es tuya");
        }

        return decision;
    }

    private static void terminar(Playthrough partida, String endingCode) {
        partida.setStatus(PlaythroughStatus.FINALIZADA);
        partida.setEndingCode(endingCode);
    }

    private static int acotar(int valor) {
        return Math.max(0, Math.min(100, valor));
    }

    private static <E extends Enum<E>> E parsear(Class<E> tipo, String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return Enum.valueOf(tipo, valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean noParseado(String valor, Enum<?> resultado) {
        return valor != null && !valor.isBlank() && resultado == null;
    }
}
