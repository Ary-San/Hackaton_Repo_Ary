package com.tuckersoft.branchengine.listener;

import com.tuckersoft.branchengine.event.DecisionCommittedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Arma y envia el Informe de Realidad. El unico sitio del sistema que toca el SMTP. */
@Component
@RequiredArgsConstructor
public class RealityReportMailer {

    private static final String REMITENTE = "branch-engine@tuckersoft.co.uk";

    private final JavaMailSender mailSender;

    public static String asunto(DecisionCommittedEvent e) {
        return "[TUCKERSOFT] " + e.branchType() + " en " + e.playerTag()
                + " | Impacto " + e.impactLevel();
    }

    public static String cuerpo(DecisionCommittedEvent e) {
        String separador = "━".repeat(36);
        return """
                Hola %s,

                Una partida de prueba acaba de ramificarse.

                %s
                Decision ID      : #%d
                Jugador          : %s
                Rama             : %s
                Impacto          : %s
                Departamento     : %s
                Consecuencia     : %s
                Nodo origen      : %s
                Nodo destino     : %s
                Estado partida   : %s
                Lucidez          : %d/100
                Nivel de control : %d/100
                Final            : %s
                Registrada       : %s
                %s

                Decisión original del jugador:
                "%s"

                — Tuckersoft Branch Engine, 1984
                """.formatted(
                e.displayName(),
                separador,
                e.decisionId(),
                e.playerTag(),
                e.branchType(),
                e.impactLevel(),
                e.handlerUnit(),
                e.outcomeCode(),
                oGuion(e.sourceNodeCode()),
                oGuion(e.resolvedNodeCode()),
                e.playthroughStatus(),
                e.lucidity(),
                e.controlLevel(),
                oGuion(e.endingCode()),
                e.createdAt(),
                separador,
                e.rawInput());
    }

    /**
     * Envia de verdad por SMTP.
     *
     * El modo QA no escribe el log FAILED a mano: lanza una excepcion aqui dentro, en
     * el mismo sitio donde reventaria un fallo real, para que la atrape el mismo catch.
     */
    public void enviar(DecisionCommittedEvent evento) {
        if (evento.simularFalloDeCorreo()) {
            throw new MailSendException(
                    "Fallo de SMTP simulado por la cabecera X-Bandersnatch-Simulate: MAIL_FAILURE");
        }

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(REMITENTE);
        mensaje.setTo(evento.recipientEmail());
        mensaje.setSubject(asunto(evento));
        mensaje.setText(cuerpo(evento));

        mailSender.send(mensaje);
    }

    private static String oGuion(String valor) {
        return valor == null ? "-" : valor;
    }
}
