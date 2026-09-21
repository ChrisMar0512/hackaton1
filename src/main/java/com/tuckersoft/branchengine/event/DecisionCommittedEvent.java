package com.tuckersoft.branchengine.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class DecisionCommittedEvent {
    private final Long decisionId;
    private final String playerTag;
    private final String branchType;
    private final String impactLevel;
    private final String handlerUnit;
    private final String outcomeCode;
    private final String sourceNodeCode;
    private final String resolvedNodeCode;
    private final String playthroughStatus;
    private final Integer lucidity;
    private final Integer controlLevel;
    private final String endingCode;
    private final Instant createdAt;
    private final String rawInput;
    private final String recipientEmail;
    private final String recipientDisplayName;
    private final String simulateHeader;
}
