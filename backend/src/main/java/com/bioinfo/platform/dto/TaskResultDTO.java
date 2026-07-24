package com.bioinfo.platform.dto;

import com.bioinfo.platform.entity.TaskResult;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TaskResultDTO {
    private Long id;
    private Long taskId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String fileType;
    private String description;
    private LocalDateTime createdAt;

    public static TaskResultDTO fromTaskResult(TaskResult result) {
        TaskResultDTO dto = new TaskResultDTO();
        dto.setId(result.getId());
        dto.setTaskId(result.getTask().getId());
        dto.setFileName(result.getFileName());
        dto.setFilePath(result.getFilePath());
        dto.setFileSize(result.getFileSize());
        dto.setFileType(result.getFileType());
        dto.setDescription(result.getDescription());
        dto.setCreatedAt(result.getCreatedAt());
        return dto;
    }
}
