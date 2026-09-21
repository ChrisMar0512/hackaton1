package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.response.RealityLogResponse;
import com.tuckersoft.branchengine.exception.ForbiddenActionException;
import com.tuckersoft.branchengine.exception.ResourceNotFoundException;
import com.tuckersoft.branchengine.model.Decision;
import com.tuckersoft.branchengine.model.RealityLog;
import com.tuckersoft.branchengine.model.User;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.RealityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RealityLogService {

    private final RealityLogRepository realityLogRepository;
    private final DecisionRepository decisionRepository;

    public List<RealityLogResponse> getLogsByDecisionId(Long decisionId, User currentUser) {
        Decision decision = decisionRepository.findById(decisionId)
                .orElseThrow(() -> new ResourceNotFoundException("Decision no encontrada con id: " + decisionId));

        boolean isAdmin = "ROLE_ADMIN".equals(currentUser.getRole());
        boolean isOwner = decision.getPlaythrough().getUser().getId().equals(currentUser.getId());
        if (!isAdmin && !isOwner) {
            throw new ForbiddenActionException("No tiene autorizacion para consultar los reality-logs de una decision ajena");
        }

        return realityLogRepository.findByDecisionOrderByCreatedAtAsc(decision).stream()
                .map(this::toResponse)
                .toList();
    }

    public RealityLogResponse toResponse(RealityLog log) {
        return RealityLogResponse.builder()
                .id(log.getId())
                .decisionId(log.getDecision().getId())
                .recipientEmail(log.getRecipientEmail())
                .subject(log.getSubject())
                .logStatus(log.getLogStatus())
                .errorMessage(log.getErrorMessage())
                .sentAt(log.getSentAt())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
