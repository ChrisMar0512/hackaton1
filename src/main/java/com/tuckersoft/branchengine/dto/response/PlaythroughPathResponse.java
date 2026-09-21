package com.tuckersoft.branchengine.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaythroughPathResponse {
    private Long playthroughId;
    private String playerTag;
    private String status;
    private String endingCode;
    private String startNodeCode;
    private String currentNodeCode;
    @Builder.Default
    private List<DecisionStepResponse> steps = new ArrayList<>();
}
