package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.request.PlaythroughRequest;
import com.tuckersoft.branchengine.dto.response.DecisionStepResponse;
import com.tuckersoft.branchengine.dto.response.PlaythroughPathResponse;
import com.tuckersoft.branchengine.dto.response.PlaythroughResponse;
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

    @Transactional
    public PlaythroughResponse createPlaythrough(PlaythroughRequest request, User currentUser) {
        StoryNode node = storyNodeRepository.findByNodeCode(request.getStartNodeCode())
                .orElseThrow(() -> new ResourceNotFoundException("Nodo de inicio no encontrado con codigo: " + request.getStartNodeCode()));

        if (playthroughRepository.existsByPlayerTag(request.getPlayerTag())) {
            throw new DuplicateResourceException("El playerTag ya se encuentra registrado: " + request.getPlayerTag());
        }

        if (node.getCurrentBranches() >= node.getBranchCapacity()) {
            throw new BusinessRuleException("El nodo inicial ha alcanzado su capacidad maxima de ramificaciones");
        }

        node.setCurrentBranches(node.getCurrentBranches() + 1);
        storyNodeRepository.save(node);

        Instant now = Instant.now();
        Playthrough playthrough = Playthrough.builder()
                .playerTag(request.getPlayerTag())
                .user(currentUser)
                .startNodeCode(node.getNodeCode())
                .currentNode(node)
                .lucidity(100)
                .controlLevel(0)
                .status("ACTIVA")
                .endingCode(null)
                .createdAt(now)
                .updatedAt(now)
                .build();

        playthroughRepository.save(playthrough);

        return toResponse(playthrough);
    }

    public List<PlaythroughResponse> getPlaythroughs(User currentUser) {
        List<Playthrough> list;
        if ("ROLE_ADMIN".equals(currentUser.getRole())) {
            list = playthroughRepository.findAllByOrderByCreatedAtDesc();
        } else {
            list = playthroughRepository.findByUserOrderByCreatedAtDesc(currentUser);
        }
        return list.stream().map(this::toResponse).toList();
    }

    public PlaythroughResponse getPlaythroughById(Long id, User currentUser) {
        Playthrough playthrough = playthroughRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partida no encontrada con id: " + id));

        validateViewAccess(playthrough, currentUser);

        return toResponse(playthrough);
    }

    public PlaythroughPathResponse getPlaythroughPath(Long id, User currentUser) {
        Playthrough playthrough = playthroughRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partida no encontrada con id: " + id));

        validateViewAccess(playthrough, currentUser);

        List<Decision> decisions = decisionRepository.findByPlaythroughAndResolvedNodeCodeIsNotNullOrderByCreatedAtAsc(playthrough);

        List<DecisionStepResponse> steps = new ArrayList<>();
        int order = 1;
        for (Decision d : decisions) {
            steps.add(DecisionStepResponse.builder()
                    .order(order++)
                    .decisionId(d.getId())
                    .fromNodeCode(d.getNode().getNodeCode())
                    .toNodeCode(d.getResolvedNodeCode())
                    .branchType(d.getBranchType())
                    .impactLevel(d.getImpactLevel())
                    .createdAt(d.getCreatedAt())
                    .build());
        }

        return PlaythroughPathResponse.builder()
                .playthroughId(playthrough.getId())
                .playerTag(playthrough.getPlayerTag())
                .status(playthrough.getStatus())
                .endingCode(playthrough.getEndingCode())
                .startNodeCode(playthrough.getStartNodeCode())
                .currentNodeCode(playthrough.getCurrentNode().getNodeCode())
                .steps(steps)
                .build();
    }

    private void validateViewAccess(Playthrough playthrough, User currentUser) {
        boolean isAdmin = "ROLE_ADMIN".equals(currentUser.getRole());
        boolean isOwner = playthrough.getUser().getId().equals(currentUser.getId());
        if (!isAdmin && !isOwner) {
            throw new ForbiddenActionException("No tiene permisos para acceder a una partida ajena");
        }
    }

    public PlaythroughResponse toResponse(Playthrough playthrough) {
        return PlaythroughResponse.builder()
                .id(playthrough.getId())
                .playerTag(playthrough.getPlayerTag())
                .ownerEmail(playthrough.getUser().getEmail())
                .startNodeCode(playthrough.getStartNodeCode())
                .currentNodeCode(playthrough.getCurrentNode().getNodeCode())
                .lucidity(playthrough.getLucidity())
                .controlLevel(playthrough.getControlLevel())
                .status(playthrough.getStatus())
                .endingCode(playthrough.getEndingCode())
                .createdAt(playthrough.getCreatedAt())
                .updatedAt(playthrough.getUpdatedAt())
                .build();
    }
}
