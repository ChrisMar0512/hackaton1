package com.tuckersoft.branchengine.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaythroughRequest {

    @NotBlank(message = "El playerTag es obligatorio")
    @Size(min = 2, max = 40, message = "El playerTag debe tener entre 2 y 40 caracteres")
    private String playerTag;

    @NotBlank(message = "El startNodeCode es obligatorio")
    private String startNodeCode;
}
