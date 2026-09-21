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
public class DecisionResponse {
    private Long id;
    private Long playthroughId;
    private String playerTag;
    private String sourceNodeCode;
    private String resolvedNodeCode;
    private String rawInput;
    private String branchType;
    private String impactLevel;
    private String handlerUnit;
    private String outcomeCode;
    private String status;
    private String playthroughStatus;
    private Integer lucidity;
    private Integer controlLevel;
    private String endingCode;
    private Instant createdAt;
    private Instant updatedAt;
}
