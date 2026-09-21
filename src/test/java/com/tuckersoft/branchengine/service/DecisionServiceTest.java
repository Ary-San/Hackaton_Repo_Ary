package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.domain.BranchType;
import com.tuckersoft.branchengine.domain.Decision;
import com.tuckersoft.branchengine.domain.DecisionStatus;
import com.tuckersoft.branchengine.domain.Playthrough;
import com.tuckersoft.branchengine.domain.PlaythroughStatus;
import com.tuckersoft.branchengine.domain.Roles;
import com.tuckersoft.branchengine.domain.StoryNode;
import com.tuckersoft.branchengine.domain.User;
import com.tuckersoft.branchengine.dto.DecisionRequest;
import com.tuckersoft.branchengine.dto.DecisionResponse;
import com.tuckersoft.branchengine.event.DecisionCommittedEvent;
import com.tuckersoft.branchengine.exception.ConflictException;
import com.tuckersoft.branchengine.exception.ForbiddenException;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.PlaythroughRepository;
import com.tuckersoft.branchengine.repository.RealityLogRepository;
import com.tuckersoft.branchengine.repository.StoryNodeRepository;
import com.tuckersoft.branchengine.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests del motor de ramas con Mockito. No levantan Spring, no tocan PostgreSQL y no
 * abren ningun socket: se ejecutan con ./mvnw test desde la raiz.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DecisionServiceTest {

    private static final String NODO_ORIGEN = "NODE-CEREAL";
    private static final String NODO_PRIMARIO = "NODE-BUS";
    private static final String NODO_GLITCH = "NODE-ESPEJO";

    @Mock
    private DecisionRepository decisionRepository;
    @Mock
    private PlaythroughRepository playthroughRepository;
    @Mock
    private StoryNodeRepository storyNodeRepository;
    @Mock
    private RealityLogRepository realityLogRepository;
    @Mock
    private AuthenticatedUser authenticatedUser;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DecisionService service;

    private User analista;
    private StoryNode origen;
    private Playthrough partida;

    @BeforeEach
    void preparar() {
        service = new DecisionService(decisionRepository, playthroughRepository,
                storyNodeRepository, realityLogRepository, authenticatedUser, eventPublisher);

        analista = User.builder()
                .id(1L)
                .email("ada@tuckersoft.test")
                .displayName("Ada Lovelace")
                .password("$2a$10$hash")
                .role(Roles.USER)
                .createdAt(Instant.now())
                .build();

        origen = StoryNode.builder()
                .id(10L)
                .nodeCode(NODO_ORIGEN)
                .title("El desayuno")
                .sceneText("Stefan debe elegir entre Sugar Puffs y Frosties.")
                .branchCapacity(5)
                .currentBranches(1)
                .primaryBranchCode(NODO_PRIMARIO)
                .glitchBranchCode(NODO_GLITCH)
                .createdAt(Instant.now())
                .build();

        partida = Playthrough.builder()
                .id(100L)
                .playerTag("STEFAN-01")
                .user(analista)
                .startNodeCode(NODO_ORIGEN)
                .currentNode(origen)
                .lucidity(100)
                .controlLevel(0)
                .status(PlaythroughStatus.ACTIVA)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(authenticatedUser.actual()).thenReturn(analista);
        when(playthroughRepository.findById(100L)).thenReturn(Optional.of(partida));
        when(playthroughRepository.save(any(Playthrough.class))).thenAnswer(i -> i.getArgument(0));
        when(decisionRepository.save(any(Decision.class))).thenAnswer(i -> {
            Decision d = i.getArgument(0);
            if (d.getId() == null) d.setId(500L);
            return d;
        });
        when(storyNodeRepository.findByNodeCode(NODO_PRIMARIO)).thenReturn(Optional.of(
                nodoSimple(11L, NODO_PRIMARIO)));
        when(storyNodeRepository.findByNodeCode(NODO_GLITCH)).thenReturn(Optional.of(
                nodoSimple(12L, NODO_GLITCH)));
    }

    // -------------------------------------------------------------------- 1

    @Test
    @DisplayName("1. La precedencia de reglas manda: 'destruye la camara' es RUPTURA_CUARTA_PARED")
    void la_regla_de_ruptura_gana_a_la_de_rebeldia() {
        DecisionResponse res = decidir("Stefan destruye la camara que lo estaba grabando.", "LEVE");

        assertEquals("RUPTURA_CUARTA_PARED", res.branchType(),
                "El texto cumple la regla 2 (camara) y la 4 (destruye): gana la 2, que va antes");
        assertEquals("Departamento Netflix", res.handlerUnit());
        assertEquals("BREAK_FOURTH_WALL", res.outcomeCode());

        // Y por ser ruptura se desvia por la rama glitch, aunque el impacto sea LEVE.
        assertEquals(NODO_GLITCH, res.resolvedNodeCode());
    }

    // -------------------------------------------------------------------- 2

    @Test
    @DisplayName("2. Un texto sin ninguna letra es ENTRADA_CORRUPTA y no modifica la partida")
    void una_entrada_corrupta_no_toca_la_partida() {
        DecisionResponse res = decidir("%%%% 01001 ### @@@ 110", "CRITICO");

        assertEquals("ENTRADA_CORRUPTA", res.branchType());
        assertEquals("ERROR", res.status());
        assertNull(res.resolvedNodeCode(), "Una entrada corrupta no resuelve ningun nodo");

        // El impacto CRITICO del request se descarta junto con la entrada.
        assertEquals(100, partida.getLucidity());
        assertEquals(0, partida.getControlLevel());
        assertEquals(PlaythroughStatus.ACTIVA, partida.getStatus());
        assertEquals(NODO_ORIGEN, partida.getCurrentNode().getNodeCode());

        verify(playthroughRepository, never()).save(any(Playthrough.class));
    }

    // -------------------------------------------------------------------- 3

    @Test
    @DisplayName("3. Un impacto CRITICO baja 40 de lucidez, sube 45 de control y respeta los limites")
    void el_impacto_critico_aplica_sus_deltas_y_acota() {
        DecisionResponse primera = decidir("Stefan sigue adelante con lo que el guion le indica.", "CRITICO");
        assertEquals(60, primera.lucidity());
        assertEquals(45, primera.controlLevel());

        // Desde 20/90, los deltas se saldrian del rango: hay que topar en 0 y en 100.
        partida.setLucidity(20);
        partida.setControlLevel(90);
        partida.setCurrentNode(origen);

        DecisionResponse segunda = decidir("Stefan sigue adelante con lo que el guion le indica.", "CRITICO");
        assertEquals(0, segunda.lucidity(), "100 - 40 - 40 - 40 daria -20: se topa en 0");
        assertEquals(100, segunda.controlLevel(), "45 + 45 + 45 daria 135: se topa en 100");
    }

    // -------------------------------------------------------------------- 4

    @Test
    @DisplayName("4. Con controlLevel en 100 el final es ENDING_PAC_SYMBOL aunque la lucidez sea 0")
    void el_final_por_control_se_evalua_antes_que_el_de_lucidez() {
        partida.setLucidity(20);
        partida.setControlLevel(90);

        DecisionResponse res = decidir("Stefan sigue adelante con lo que el guion le indica.", "CRITICO");

        assertEquals(0, res.lucidity());
        assertEquals(100, res.controlLevel());
        assertEquals("FINALIZADA", res.playthroughStatus());
        assertEquals(DecisionService.ENDING_PAC_SYMBOL, res.endingCode(),
                "Las dos condiciones se cumplen a la vez: gana la del control, que se evalua primera");

        // La partida termina donde estaba: currentNode no se mueve.
        assertEquals(NODO_ORIGEN, partida.getCurrentNode().getNodeCode());
    }

    // -------------------------------------------------------------------- 5

    @Test
    @DisplayName("5. El evento se publica una vez en una decision normal y ninguna en una corrupta")
    void el_evento_solo_se_publica_cuando_la_decision_cuenta() {
        decidir("Stefan acepta la oferta de Mohan y se queda en Tuckersoft.", "LEVE");

        ArgumentCaptor<DecisionCommittedEvent> captor =
                ArgumentCaptor.forClass(DecisionCommittedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());

        DecisionCommittedEvent evento = captor.getValue();
        assertEquals("ada@tuckersoft.test", evento.recipientEmail(),
                "El informe va al dueno de la partida");
        assertEquals("OBEDIENCIA", evento.branchType());

        decidir("### 0101 @@@ 11", "LEVE");
        verify(eventPublisher, times(1)).publishEvent(any(DecisionCommittedEvent.class));
    }

    // ------------------------------------------------------- extras de apoyo

    @Test
    @DisplayName("6. El nodo destino sale de glitchBranchCode cuando el impacto es CRITICO")
    void un_impacto_critico_desvia_por_la_rama_glitch() {
        DecisionResponse res = decidir("Stefan acepta la oferta y sigue el guion previsto.", "CRITICO");

        assertEquals("OBEDIENCIA", res.branchType());
        assertEquals(NODO_GLITCH, res.resolvedNodeCode(),
                "La condicion es rama RUPTURA_CUARTA_PARED O impacto CRITICO");
    }

    @Test
    @DisplayName("7. Sin nodo destino la partida termina en ENDING_NETFLIX_CUT")
    void una_rama_que_no_lleva_a_ningun_lado_corta_la_partida() {
        origen.setPrimaryBranchCode(null);

        DecisionResponse res = decidir("Stefan acepta la oferta y sigue el guion previsto.", "LEVE");

        assertNull(res.resolvedNodeCode());
        assertEquals("FINALIZADA", res.playthroughStatus());
        assertEquals(DecisionService.ENDING_NETFLIX_CUT, res.endingCode());
    }

    @Test
    @DisplayName("8. Una partida FINALIZADA no acepta mas decisiones")
    void una_partida_terminada_responde_conflicto() {
        partida.setStatus(PlaythroughStatus.FINALIZADA);
        partida.setEndingCode(DecisionService.ENDING_WHITE_BEAR);

        assertThrows(ConflictException.class,
                () -> decidir("Stefan intenta seguir jugando despues del final.", "LEVE"));
    }

    @Test
    @DisplayName("9. Nadie decide sobre una partida ajena, ni siquiera el administrador")
    void una_partida_ajena_no_se_toca() {
        User supervisor = User.builder()
                .id(99L)
                .email("colin@tuckersoft.co.uk")
                .displayName("Colin Ritman")
                .password("$2a$10$hash")
                .role(Roles.ADMIN)
                .createdAt(Instant.now())
                .build();
        when(authenticatedUser.actual()).thenReturn(supervisor);

        assertThrows(ForbiddenException.class,
                () -> decidir("El administrador intenta decidir sobre una partida ajena.", "LEVE"));
    }

    @Test
    @DisplayName("10. Una decision normal nace REGISTRADA: el listener la mueve despues")
    void la_decision_nace_registrada() {
        DecisionResponse res = decidir("Stefan acepta la oferta y sigue el guion previsto.", "LEVE");
        assertEquals(DecisionStatus.REGISTRADA.name(), res.status());
        assertEquals(BranchType.OBEDIENCIA.getHandlerUnit(), res.handlerUnit());
        assertEquals(NODO_PRIMARIO, partida.getCurrentNode().getNodeCode(),
                "La partida se mueve al nodo que resolvio la rama");
    }

    // ------------------------------------------------------------- utilidades

    private DecisionResponse decidir(String texto, String impacto) {
        return service.registrar(new DecisionRequest(100L, texto, impacto), null);
    }

    private static StoryNode nodoSimple(Long id, String codigo) {
        return StoryNode.builder()
                .id(id)
                .nodeCode(codigo)
                .title("Escena " + codigo)
                .sceneText("Escena de prueba para " + codigo + ".")
                .branchCapacity(50)
                .currentBranches(0)
                .createdAt(Instant.now())
                .build();
    }
}
