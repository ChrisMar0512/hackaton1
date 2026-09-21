package com.tuckersoft.branchengine.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DecisionRequest {

    @NotNull(message = "El playthroughId es obligatorio")
    private Long playthroughId;

    @NotBlank(message = "El rawInput es obligatorio")
    @Size(min = 10, message = "El rawInput debe tener al menos 10 caracteres")
    private String rawInput;

    @NotBlank(message = "El impactLevel es obligatorio")
    private String impactLevel;
}
