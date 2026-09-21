package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.request.DecisionRequest;
import com.tuckersoft.branchengine.dto.response.DecisionResponse;
import com.tuckersoft.branchengine.dto.response.PageResponse;
import com.tuckersoft.branchengine.event.DecisionCommittedEvent;
import com.tuckersoft.branchengine.exception.BusinessRuleException;
import com.tuckersoft.branchengine.exception.DuplicateResourceException;
import com.tuckersoft.branchengine.exception.ForbiddenActionException;
import com.tuckersoft.branchengine.exception.ResourceNotFoundException;
import com.tuckersoft.branchengine.model.Decision;
import com.tuckersoft.branchengine.model.Playthrough;
import com.tuckersoft.branchengine.model.StoryNode;
import com.tuckersoft.branchengine.model.User;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.PlaythroughRepository;
import com.tuckersoft.branchengine.repository.StoryNodeRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DecisionService {

    private final DecisionRepository decisionRepository;
    private final PlaythroughRepository playthroughRepository;
    private final StoryNodeRepository storyNodeRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final Set<String> VALID_IMPACT_LEVELS = Set.of("LEVE", "MODERADO", "GRAVE", "CRITICO");

    @Transactional
    public DecisionResponse createDecision(DecisionRequest request, User currentUser, String simulateHeader) {
        if (!VALID_IMPACT_LEVELS.contains(request.getImpactLevel())) {
            throw new BusinessRuleException("impactLevel no valido: " + request.getImpactLevel());
        }

        Playthrough playthrough = playthroughRepository.findById(request.getPlaythroughId())
                .orElseThrow(() -> new ResourceNotFoundException("Partida no encontrada con id: " + request.getPlaythroughId()));

        // Solo el dueño puede decidir, incluso si quien lo pide es ROLE_ADMIN
        if (!playthrough.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenActionException("No tiene autorizacion para decidir sobre una partida ajena");
        }

        if ("FINALIZADA".equals(playthrough.getStatus())) {
            throw new DuplicateResourceException("La partida ya se encuentra FINALIZADA y no acepta mas decisiones");
        }

        StoryNode sourceNode = playthrough.getCurrentNode();
        String branchType = classifyBranch(request.getRawInput());
        String handlerUnit = deriveHandlerUnit(branchType);
        String outcomeCode = deriveOutcomeCode(branchType);

        Instant now = Instant.now();

        // Si es ENTRADA_CORRUPTA: se guarda con ERROR, sin tocar la partida ni publicar evento
        if ("ENTRADA_CORRUPTA".equals(branchType)) {
            Decision decision = Decision.builder()
                    .playthrough(playthrough)
                    .node(sourceNode)
                    .rawInput(request.getRawInput())
                    .branchType(branchType)
                    .impactLevel(request.getImpactLevel())
                    .handlerUnit(handlerUnit)
                    .outcomeCode(outcomeCode)
                    .resolvedNodeCode(null)
                    .status("ERROR")
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            decisionRepository.save(decision);

            return toResponse(decision, playthrough);
        }

        // Aplicar stats de impacto
        applyImpactStats(playthrough, request.getImpactLevel());

        // Resolver nodo destino
        boolean useGlitch = "RUPTURA_CUARTA_PARED".equals(branchType) || "CRITICO".equals(request.getImpactLevel());
        String resolvedNodeCode = useGlitch ? sourceNode.getGlitchBranchCode() : sourceNode.getPrimaryBranchCode();

        // Evaluar estado de la partida en orden estricto:
        // 1. controlLevel >= 100 -> ENDING_PAC_SYMBOL (gana al final por lucidez)
        // 2. lucidity <= 0 -> ENDING_WHITE_BEAR
        // 3. código destino es null o no existe nodo en BD -> ENDING_NETFLIX_CUT
        // 4. normal -> ACTIVA y currentNode se mueve
        if (playthrough.getControlLevel() >= 100) {
            playthrough.setStatus("FINALIZADA");
            playthrough.setEndingCode("ENDING_PAC_SYMBOL");
        } else if (playthrough.getLucidity() <= 0) {
            playthrough.setStatus("FINALIZADA");
            playthrough.setEndingCode("ENDING_WHITE_BEAR");
        } else {
            Optional<StoryNode> targetNodeOpt = (resolvedNodeCode != null)
                    ? storyNodeRepository.findByNodeCode(resolvedNodeCode)
                    : Optional.empty();

            if (targetNodeOpt.isEmpty()) {
                playthrough.setStatus("FINALIZADA");
                playthrough.setEndingCode("ENDING_NETFLIX_CUT");
            } else {
                playthrough.setStatus("ACTIVA");
                playthrough.setCurrentNode(targetNodeOpt.get());
            }
        }

        playthrough.setUpdatedAt(now);
        playthroughRepository.save(playthrough);

        Decision decision = Decision.builder()
                .playthrough(playthrough)
                .node(sourceNode)
                .rawInput(request.getRawInput())
                .branchType(branchType)
                .impactLevel(request.getImpactLevel())
                .handlerUnit(handlerUnit)
                .outcomeCode(outcomeCode)
                .resolvedNodeCode(resolvedNodeCode)
                .status("REGISTRADA")
                .createdAt(now)
                .updatedAt(now)
                .build();

        decision = decisionRepository.save(decision);

        // Publicar evento para procesamiento asíncrono y envío de correos
        DecisionCommittedEvent event = DecisionCommittedEvent.builder()
                .decisionId(decision.getId())
                .playerTag(playthrough.getPlayerTag())
                .branchType(branchType)
                .impactLevel(request.getImpactLevel())
                .handlerUnit(handlerUnit)
                .outcomeCode(outcomeCode)
                .sourceNodeCode(sourceNode.getNodeCode())
                .resolvedNodeCode(resolvedNodeCode)
                .playthroughStatus(playthrough.getStatus())
                .lucidity(playthrough.getLucidity())
                .controlLevel(playthrough.getControlLevel())
                .endingCode(playthrough.getEndingCode())
                .createdAt(now)
                .rawInput(request.getRawInput())
                .recipientEmail(playthrough.getUser().getEmail())
                .recipientDisplayName(playthrough.getUser().getDisplayName())
                .simulateHeader(simulateHeader)
                .build();

        eventPublisher.publishEvent(event);

        return toResponse(decision, playthrough);
    }

    public PageResponse<DecisionResponse> getDecisions(
            String branchType,
            String impactLevel,
            String status,
            Long playthroughId,
            Integer page,
            Integer size,
            User currentUser
    ) {
        int pageNumber = (page != null && page >= 0) ? page : 0;
        int pageSize = (size != null && size > 0) ? size : 10;
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("createdAt").descending());

        Specification<Decision> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Aislamiento por usuario
            if (!"ROLE_ADMIN".equals(currentUser.getRole())) {
                predicates.add(cb.equal(root.get("playthrough").get("user").get("id"), currentUser.getId()));
            }

            if (branchType != null && !branchType.isBlank()) {
                predicates.add(cb.equal(root.get("branchType"), branchType));
            }
            if (impactLevel != null && !impactLevel.isBlank()) {
                predicates.add(cb.equal(root.get("impactLevel"), impactLevel));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (playthroughId != null) {
                predicates.add(cb.equal(root.get("playthrough").get("id"), playthroughId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Decision> decisionPage = decisionRepository.findAll(spec, pageable);

        List<DecisionResponse> content = decisionPage.getContent().stream()
                .map(d -> toResponse(d, d.getPlaythrough()))
                .toList();

        return PageResponse.<DecisionResponse>builder()
                .content(content)
                .totalElements(decisionPage.getTotalElements())
                .totalPages(decisionPage.getTotalPages())
                .currentPage(pageNumber)
                .size(pageSize)
                .build();
    }

    public DecisionResponse getDecisionById(Long id, User currentUser) {
        Decision decision = decisionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Decision no encontrada con id: " + id));

        boolean isAdmin = "ROLE_ADMIN".equals(currentUser.getRole());
        boolean isOwner = decision.getPlaythrough().getUser().getId().equals(currentUser.getId());
        if (!isAdmin && !isOwner) {
            throw new ForbiddenActionException("No tiene permisos para ver una decision ajena");
        }

        return toResponse(decision, decision.getPlaythrough());
    }

    public String classifyBranch(String rawInput) {
        String normalized = Normalizer.normalize(rawInput, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();

        // 1. No contiene ninguna letra de la a a la z
        if (!normalized.matches(".*[a-z].*")) {
            return "ENTRADA_CORRUPTA";
        }

        // 2. Contiene netflix, camara, espectador o videojuego
        if (containsAny(normalized, "netflix", "camara", "espectador", "videojuego")) {
            return "RUPTURA_CUARTA_PARED";
        }

        // 3. Contiene vigilan, simbolo o conspiracion
        if (containsAny(normalized, "vigilan", "simbolo", "conspiracion")) {
            return "SOSPECHA";
        }

        // 4. Contiene rechaza, destruye, desobedece o renuncia
        if (containsAny(normalized, "rechaza", "destruye", "desobedece", "renuncia")) {
            return "REBELDIA";
        }

        // 5. Cualquier otro caso
        return "OBEDIENCIA";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    public String deriveHandlerUnit(String branchType) {
        return switch (branchType) {
            case "OBEDIENCIA" -> "Mesa de Guion";
            case "REBELDIA" -> "Control de Continuidad";
            case "SOSPECHA" -> "Oficina de Seguridad";
            case "RUPTURA_CUARTA_PARED" -> "Departamento Netflix";
            case "ENTRADA_CORRUPTA" -> "Archivo de Errores";
            default -> "Mesa de Guion";
        };
    }

    public String deriveOutcomeCode(String branchType) {
        return switch (branchType) {
            case "OBEDIENCIA" -> "ADVANCE_MAIN_PATH";
            case "REBELDIA" -> "FORK_TIMELINE";
            case "SOSPECHA" -> "INJECT_WHITE_BEAR_SYMBOL";
            case "RUPTURA_CUARTA_PARED" -> "BREAK_FOURTH_WALL";
            case "ENTRADA_CORRUPTA" -> "DISCARD_INPUT";
            default -> "ADVANCE_MAIN_PATH";
        };
    }

    private void applyImpactStats(Playthrough playthrough, String impactLevel) {
        int deltaLucidity = switch (impactLevel) {
            case "LEVE" -> -5;
            case "MODERADO" -> -15;
            case "GRAVE" -> -30;
            case "CRITICO" -> -40;
            default -> 0;
        };

        int deltaControl = switch (impactLevel) {
            case "LEVE" -> 5;
            case "MODERADO" -> 10;
            case "GRAVE" -> 20;
            case "CRITICO" -> 45;
            default -> 0;
        };

        int newLucidity = Math.max(0, Math.min(100, playthrough.getLucidity() + deltaLucidity));
        int newControl = Math.max(0, Math.min(100, playthrough.getControlLevel() + deltaControl));

        playthrough.setLucidity(newLucidity);
        playthrough.setControlLevel(newControl);
    }

    public DecisionResponse toResponse(Decision decision, Playthrough playthrough) {
        return DecisionResponse.builder()
                .id(decision.getId())
                .playthroughId(playthrough.getId())
                .playerTag(playthrough.getPlayerTag())
                .sourceNodeCode(decision.getNode().getNodeCode())
                .resolvedNodeCode(decision.getResolvedNodeCode())
                .rawInput(decision.getRawInput())
                .branchType(decision.getBranchType())
                .impactLevel(decision.getImpactLevel())
                .handlerUnit(decision.getHandlerUnit())
                .outcomeCode(decision.getOutcomeCode())
                .status(decision.getStatus())
                .playthroughStatus(playthrough.getStatus())
                .lucidity(playthrough.getLucidity())
                .controlLevel(playthrough.getControlLevel())
                .endingCode(playthrough.getEndingCode())
                .createdAt(decision.getCreatedAt())
                .updatedAt(decision.getUpdatedAt())
                .build();
    }
}
