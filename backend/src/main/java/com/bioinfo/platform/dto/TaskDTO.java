package com.bioinfo.platform.dto;

import com.bioinfo.platform.entity.Task;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class TaskDTO {
    private Long id;
    private String name;
    private Long analysisTypeId;
    private String analysisTypeName;
    private String status;
    private String priority;
    private String description;
    private String inputFiles;
    private String outputDir;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int parameterCount;
    private int logCount;
    private int resultCount;
    private List<TaskFileDTO> inputFileList;

    public static TaskDTO fromTask(Task task) {
        TaskDTO dto = new TaskDTO();
        dto.setId(task.getId());
        dto.setName(task.getName());
        if (task.getAnalysisType() != null) {
            dto.setAnalysisTypeId(task.getAnalysisType().getId());
            dto.setAnalysisTypeName(task.getAnalysisType().getName());
        }
        dto.setStatus(task.getStatus());
        dto.setPriority(task.getPriority());
        dto.setDescription(task.getDescription());
        dto.setInputFiles(task.getInputFiles());
        dto.setOutputDir(task.getOutputDir());
        dto.setErrorMessage(task.getErrorMessage());
        dto.setStartedAt(task.getStartedAt());
        dto.setFinishedAt(task.getFinishedAt());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setUpdatedAt(task.getUpdatedAt());
        dto.setParameterCount(task.getParameters() != null ? task.getParameters().size() : 0);
        dto.setLogCount(task.getLogs() != null ? task.getLogs().size() : 0);
        dto.setResultCount(task.getResults() != null ? task.getResults().size() : 0);
        if (task.getFiles() != null) {
            dto.setInputFileList(
                task.getFiles().stream()
                    .map(TaskFileDTO::fromTaskFile)
                    .collect(Collectors.toList())
            );
        }
        return dto;
    }
}
