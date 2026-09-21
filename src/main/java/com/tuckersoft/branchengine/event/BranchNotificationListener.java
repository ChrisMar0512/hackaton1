package com.tuckersoft.branchengine.event;

import com.tuckersoft.branchengine.model.Decision;
import com.tuckersoft.branchengine.model.RealityLog;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.RealityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class BranchNotificationListener {

    private final DecisionRepository decisionRepository;
    private final RealityLogRepository realityLogRepository;
    private final JavaMailSender mailSender;

    @Async("branchExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDecisionCommitted(DecisionCommittedEvent event) {
        Decision decision = decisionRepository.findById(event.getDecisionId()).orElse(null);
        if (decision == null) {
            log.warn("No se encontró la decisión #{} en el listener", event.getDecisionId());
            return;
        }

        decision.setStatus("PROCESANDO");
        decision.setUpdatedAt(Instant.now());
        decision = decisionRepository.save(decision);

        String subject = "[TUCKERSOFT] " + event.getBranchType() + " en " + event.getPlayerTag() + " | Impacto " + event.getImpactLevel();
        String endingDisplay = event.getEndingCode() != null ? event.getEndingCode() : "-";
        String resolvedNodeDisplay = event.getResolvedNodeCode() != null ? event.getResolvedNodeCode() : "-";

        String body = String.format(
                "Hola %s,\n\n" +
                "Una partida de prueba acaba de ramificarse.\n\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "Decision ID      : #%d\n" +
                "Jugador          : %s\n" +
                "Rama             : %s\n" +
                "Impacto          : %s\n" +
                "Departamento     : %s\n" +
                "Consecuencia     : %s\n" +
                "Nodo origen      : %s\n" +
                "Nodo destino     : %s\n" +
                "Estado partida   : %s\n" +
                "Lucidez          : %d/100\n" +
                "Nivel de control : %d/100\n" +
                "Final            : %s\n" +
                "Registrada       : %s\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                "Decisión original del jugador:\n" +
                "\"%s\"\n\n" +
                "— Tuckersoft Branch Engine, 1984\n",
                event.getRecipientDisplayName(),
                event.getDecisionId(),
                event.getPlayerTag(),
                event.getBranchType(),
                event.getImpactLevel(),
                event.getHandlerUnit(),
                event.getOutcomeCode(),
                event.getSourceNodeCode(),
                resolvedNodeDisplay,
                event.getPlaythroughStatus(),
                event.getLucidity(),
                event.getControlLevel(),
                endingDisplay,
                event.getCreatedAt(),
                event.getRawInput()
        );

        try {
            if ("MAIL_FAILURE".equals(event.getSimulateHeader())) {
                throw new RuntimeException("Fallo de envío SMTP simulado vía X-Bandersnatch-Simulate: MAIL_FAILURE");
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(event.getRecipientEmail());
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);

            decision.setStatus("ESTABILIZADA");
            decision.setUpdatedAt(Instant.now());
            decisionRepository.save(decision);

            RealityLog realityLog = RealityLog.builder()
                    .decision(decision)
                    .recipientEmail(event.getRecipientEmail())
                    .subject(subject)
                    .logStatus("SENT")
                    .errorMessage(null)
                    .sentAt(Instant.now())
                    .createdAt(Instant.now())
                    .build();
            realityLogRepository.save(realityLog);

        } catch (Exception e) {
            log.error("Error al procesar notificación de decisión #{}: {}", event.getDecisionId(), e.getMessage());

            decision.setStatus("ERROR");
            decision.setUpdatedAt(Instant.now());
            decisionRepository.save(decision);

            RealityLog realityLog = RealityLog.builder()
                    .decision(decision)
                    .recipientEmail(event.getRecipientEmail())
                    .subject(subject)
                    .logStatus("FAILED")
                    .errorMessage(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())
                    .sentAt(null)
                    .createdAt(Instant.now())
                    .build();
            realityLogRepository.save(realityLog);
        }

        String threadName = Thread.currentThread().getName();
        System.out.printf("[BRANCH-LOG] Decision ID: %d | Player: %s | Branch: %s | Impact: %s | Unit: %s | Node: %s -> %s | Thread: %s | Status: %s%n",
                event.getDecisionId(),
                event.getPlayerTag(),
                event.getBranchType(),
                event.getImpactLevel(),
                event.getHandlerUnit(),
                event.getSourceNodeCode(),
                event.getResolvedNodeCode(),
                threadName,
                decision.getStatus()
        );
    }
}
