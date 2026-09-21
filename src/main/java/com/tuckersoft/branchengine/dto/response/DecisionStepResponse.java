package com.tuckersoft.branchengine.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionStepResponse {
    private Integer order;
    private Long decisionId;
    private String fromNodeCode;
    private String toNodeCode;
    private String branchType;
    private String impactLevel;
    private Instant createdAt;
}
