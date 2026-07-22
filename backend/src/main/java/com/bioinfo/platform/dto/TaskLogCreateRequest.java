package com.bioinfo.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TaskLogCreateRequest {
    @NotBlank(message = "Log level is required")
    private String level;

    @NotBlank(message = "Log message is required")
    private String message;
}
