package com.bioinfo.platform.service;

import com.bioinfo.platform.entity.Task;
import com.bioinfo.platform.entity.TaskLog;
import com.bioinfo.platform.repository.TaskLogRepository;
import com.bioinfo.platform.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TaskLogService {

    private final TaskLogRepository taskLogRepository;
    private final TaskRepository taskRepository;

    @Transactional
    public void addLog(Long taskId, String level, String message) {
        Task task = taskRepository.findById(taskId).orElse(null);
        if (task == null) return;

        TaskLog log = new TaskLog();
        log.setTask(task);
        log.setLevel(level);
        log.setMessage(message);
        taskLogRepository.save(log);
    }

    @Transactional
    public void info(Long taskId, String message) {
        addLog(taskId, "INFO", message);
    }

    @Transactional
    public void warn(Long taskId, String message) {
        addLog(taskId, "WARN", message);
    }

    @Transactional
    public void error(Long taskId, String message) {
        addLog(taskId, "ERROR", message);
    }
}
