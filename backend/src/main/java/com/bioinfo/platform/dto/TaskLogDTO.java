package com.bioinfo.platform.dto;

import com.bioinfo.platform.entity.TaskLog;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TaskLogDTO {
    private Long id;
    private Long taskId;
    private String level;
    private String message;
    private LocalDateTime createdAt;

    public static TaskLogDTO fromTaskLog(TaskLog log) {
        TaskLogDTO dto = new TaskLogDTO();
        dto.setId(log.getId());
        dto.setTaskId(log.getTask().getId());
        dto.setLevel(log.getLevel());
        dto.setMessage(log.getMessage());
        dto.setCreatedAt(log.getCreatedAt());
        return dto;
    }
}
