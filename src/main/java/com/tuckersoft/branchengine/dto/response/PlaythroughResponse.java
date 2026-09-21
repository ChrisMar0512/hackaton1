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
public class PlaythroughResponse {
    private Long id;
    private String playerTag;
    private String ownerEmail;
    private String startNodeCode;
    private String currentNodeCode;
    private Integer lucidity;
    private Integer controlLevel;
    private String status;
    private String endingCode;
    private Instant createdAt;
    private Instant updatedAt;
}
