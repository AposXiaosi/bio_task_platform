package com.bioinfo.platform.dto;

import com.bioinfo.platform.entity.TaskFile;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TaskFileDTO {
    private Long id;
    private Long taskId;
    private String originalName;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String fileType;
    private LocalDateTime createdAt;

    public static TaskFileDTO fromTaskFile(TaskFile file) {
        TaskFileDTO dto = new TaskFileDTO();
        dto.setId(file.getId());
        dto.setTaskId(file.getTask() != null ? file.getTask().getId() : null);
        dto.setOriginalName(file.getOriginalName());
        dto.setFileName(file.getFileName());
        dto.setFilePath(file.getFilePath());
        dto.setFileSize(file.getFileSize());
        dto.setFileType(file.getFileType());
        dto.setCreatedAt(file.getCreatedAt());
        return dto;
    }
}
