package com.tuckersoft.branchengine.listener;

import com.tuckersoft.branchengine.domain.Decision;
import com.tuckersoft.branchengine.domain.DecisionStatus;
import com.tuckersoft.branchengine.domain.LogStatus;
import com.tuckersoft.branchengine.domain.RealityLog;
import com.tuckersoft.branchengine.event.DecisionCommittedEvent;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.RealityLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

/**
 * El lado asincrono del motor: manda el Informe de Realidad y lo audita.
 *
 * Vive en un @Component aparte del DecisionService a proposito. Spring no aplica
 * @Async a una llamada interna del mismo bean, asi que si esto estuviera dentro del
 * service el correo se enviaria en el hilo de la peticion y el 201 tardaria segundos.
 *
 * Con phase = AFTER_COMMIT el listener solo arranca cuando PostgreSQL ya confirmo la
 * transaccion; con @EventListener podria buscar una decision que todavia no existe.
 * Y necesita REQUIRES_NEW porque la transaccion original ya se cerro: sin ella Spring
 * ni siquiera deja arrancar la aplicacion.
 */
@Component
@RequiredArgsConstructor
public class BranchNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(BranchNotificationListener.class);

    private final DecisionRepository decisionRepository;
    private final RealityLogRepository realityLogRepository;
    private final RealityReportMailer mailer;

    @Async("branchExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCommit(DecisionCommittedEvent evento) {
        Decision decision = decisionRepository.findById(evento.decisionId()).orElse(null);
        if (decision == null) {
            log.error("[BRANCH-LOG] La decision #{} desaparecio antes de notificarla",
                    evento.decisionId());
            return;
        }

        String asunto = RealityReportMailer.asunto(evento);

        // 7. La decision entra en proceso.
        decision.setStatus(DecisionStatus.PROCESANDO);
        decision.setUpdatedAt(Instant.now());
        decisionRepository.saveAndFlush(decision);

        RealityLog registro = RealityLog.builder()
                .decision(decision)
                .recipientEmail(evento.recipientEmail())
                .subject(asunto)
                .createdAt(Instant.now())
                .build();

        try {
            // 8. Envio real por SMTP.
            mailer.enviar(evento);

            decision.setStatus(DecisionStatus.ESTABILIZADA);
            registro.setLogStatus(LogStatus.SENT);
            registro.setSentAt(Instant.now());
            registro.setErrorMessage(null);

        } catch (Exception e) {
            decision.setStatus(DecisionStatus.ERROR);
            registro.setLogStatus(LogStatus.FAILED);
            registro.setSentAt(null);
            registro.setErrorMessage(e.getMessage() == null ? e.toString() : e.getMessage());

            log.error("[BRANCH-LOG] Fallo el envio del Informe de Realidad de la decision #{}: {}",
                    decision.getId(), e.getMessage());
        }

        decision.setUpdatedAt(Instant.now());
        decisionRepository.save(decision);
        realityLogRepository.save(registro);

        // 9. El hilo tiene que ser branch-worker-N. Si sale http-nio-..., el @Async no corre.
        log.info("[BRANCH-LOG] Decision ID: {} | Player: {} | Branch: {} | Impact: {} | Unit: {} "
                        + "| Node: {} -> {} | Thread: {} | Status: {}",
                decision.getId(), evento.playerTag(), evento.branchType(), evento.impactLevel(),
                evento.handlerUnit(), evento.sourceNodeCode(), evento.resolvedNodeCode(),
                Thread.currentThread().getName(), decision.getStatus());
    }
}
