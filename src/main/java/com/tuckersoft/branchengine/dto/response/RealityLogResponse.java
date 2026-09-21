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
public class RealityLogResponse {
    private Long id;
    private Long decisionId;
    private String recipientEmail;
    private String subject;
    private String logStatus;
    private String errorMessage;
    private Instant sentAt;
    private Instant createdAt;
}
