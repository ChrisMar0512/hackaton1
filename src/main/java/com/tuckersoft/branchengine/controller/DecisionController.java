package com.tuckersoft.branchengine.controller;

import com.tuckersoft.branchengine.dto.request.DecisionRequest;
import com.tuckersoft.branchengine.dto.response.DecisionResponse;
import com.tuckersoft.branchengine.dto.response.PageResponse;
import com.tuckersoft.branchengine.dto.response.RealityLogResponse;
import com.tuckersoft.branchengine.model.User;
import com.tuckersoft.branchengine.service.DecisionService;
import com.tuckersoft.branchengine.service.RealityLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/decisions")
@RequiredArgsConstructor
public class DecisionController {

    private final DecisionService decisionService;
    private final RealityLogService realityLogService;

    @PostMapping
    public ResponseEntity<DecisionResponse> createDecision(
            @Valid @RequestBody DecisionRequest request,
            @RequestHeader(value = "X-Bandersnatch-Simulate", required = false) String simulateHeader,
            @AuthenticationPrincipal User currentUser
    ) {
        DecisionResponse response = decisionService.createDecision(request, currentUser, simulateHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<DecisionResponse>> getDecisions(
            @RequestParam(required = false) String branchType,
            @RequestParam(required = false) String impactLevel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long playthroughId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @AuthenticationPrincipal User currentUser
    ) {
        PageResponse<DecisionResponse> response = decisionService.getDecisions(
                branchType, impactLevel, status, playthroughId, page, size, currentUser
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DecisionResponse> getDecisionById(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        DecisionResponse response = decisionService.getDecisionById(id, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/reality-logs")
    public ResponseEntity<List<RealityLogResponse>> getRealityLogs(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        List<RealityLogResponse> responses = realityLogService.getLogsByDecisionId(id, currentUser);
        return ResponseEntity.ok(responses);
    }
}
