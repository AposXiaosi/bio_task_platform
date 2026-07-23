package com.bioinfo.platform.service;

import com.bioinfo.platform.dto.*;
import java.util.List;

public interface TaskService {
    TaskDTO createTask(TaskCreateRequest request);

    TaskDTO getTaskById(Long id);

    PageResponse<TaskDTO> getTaskList(int page, int size, String status, Long analysisTypeId, String keyword);

    TaskDTO updateTask(Long id, TaskUpdateRequest request);

    void deleteTask(Long id);

    TaskDTO cancelTask(Long id);

    TaskDTO completeTask(Long id);

    TaskLogDTO addTaskLog(Long taskId, TaskLogCreateRequest request);

    List<TaskLogDTO> getTaskLogs(Long taskId);

    void executeTask(Long taskId);
}
