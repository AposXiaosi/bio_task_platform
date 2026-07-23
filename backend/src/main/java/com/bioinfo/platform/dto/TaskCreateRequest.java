package com.bioinfo.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class TaskCreateRequest {
    @NotBlank(message = "Task name is required")
    private String name;

    @NotNull(message = "Analysis type ID is required")
    private Long analysisTypeId;

    private String description;

    private String priority;

    private String inputFiles;

    private List<Long> fileIds;

    private Map<String, String> parameters;
}
