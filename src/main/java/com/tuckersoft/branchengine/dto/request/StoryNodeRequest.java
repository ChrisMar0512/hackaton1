package com.tuckersoft.branchengine.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoryNodeRequest {

    @NotBlank(message = "El nodeCode es obligatorio")
    @Size(min = 3, max = 40, message = "El nodeCode debe tener entre 3 y 40 caracteres")
    private String nodeCode;

    @NotBlank(message = "El title es obligatorio")
    @Size(min = 3, max = 80, message = "El title debe tener entre 3 y 80 caracteres")
    private String title;

    @NotBlank(message = "El sceneText es obligatorio")
    @Size(min = 10, message = "El sceneText debe tener al menos 10 caracteres")
    private String sceneText;

    @NotNull(message = "El branchCapacity es obligatorio")
    @Min(value = 1, message = "El branchCapacity debe ser al menos 1")
    private Integer branchCapacity;

    private String primaryBranchCode;

    private String glitchBranchCode;
}
