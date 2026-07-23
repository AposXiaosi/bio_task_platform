package com.bioinfo.platform.service;

import com.bioinfo.platform.dto.*;
import com.bioinfo.platform.entity.*;
import com.bioinfo.platform.enums.TaskStatus;
import com.bioinfo.platform.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final AnalysisTypeRepository analysisTypeRepository;
    private final TaskParameterRepository taskParameterRepository;
    private final TaskLogRepository taskLogRepository;
    private final TaskResultRepository taskResultRepository;
    private final TaskLogService taskLogService;
    private final TaskExecutionService taskExecutionService;

    @Override
    @Transactional
    public TaskDTO createTask(TaskCreateRequest request) {
        AnalysisType analysisType = analysisTypeRepository.findById(request.getAnalysisTypeId())
                .orElseThrow(() -> new EntityNotFoundException("AnalysisType not found with id: " + request.getAnalysisTypeId()));

        Task task = new Task();
        task.setName(request.getName());
        task.setAnalysisType(analysisType);
        task.setStatus(TaskStatus.PENDING.name());
        task.setPriority(request.getPriority() != null ? request.getPriority() : "MEDIUM");
        task.setDescription(request.getDescription());
        task.setInputFiles(request.getInputFiles());

        task = taskRepository.save(task);

        if (request.getParameters() != null) {
            for (Map.Entry<String, String> entry : request.getParameters().entrySet()) {
                TaskParameter param = new TaskParameter();
                param.setTask(task);
                param.setParamName(entry.getKey());
                param.setParamValue(entry.getValue());
                param.setParamType("STRING");
                taskParameterRepository.save(param);
            }
        }

        taskLogService.info(task.getId(), "任务已创建");

        return TaskDTO.fromTask(task);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDTO getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Task not found with id: " + id));
        return TaskDTO.fromTask(task);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TaskDTO> getTaskList(int page, int size, String status, Long analysisTypeId, String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Task> taskPage;

        if (status != null && analysisTypeId != null) {
            taskPage = taskRepository.findByStatusAndAnalysisTypeId(status, analysisTypeId, pageable);
        } else if (status != null) {
            taskPage = taskRepository.findByStatus(status, pageable);
        } else if (analysisTypeId != null) {
            taskPage = taskRepository.findByAnalysisTypeId(analysisTypeId, pageable);
        } else if (keyword != null && !keyword.isEmpty()) {
            taskPage = taskRepository.findByNameContaining(keyword, pageable);
        } else {
            taskPage = taskRepository.findAll(pageable);
        }

        List<TaskDTO> dtos = taskPage.getContent().stream()
                .map(TaskDTO::fromTask)
                .collect(Collectors.toList());

        return new PageResponse<>(
                dtos,
                taskPage.getTotalElements(),
                taskPage.getTotalPages(),
                taskPage.getNumber(),
                taskPage.getSize()
        );
    }

    @Override
    @Transactional
    public TaskDTO updateTask(Long id, TaskUpdateRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Task not found with id: " + id));

        if (request.getName() != null) {
            task.setName(request.getName());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }

        task = taskRepository.save(task);
        return TaskDTO.fromTask(task);
    }

    @Override
    @Transactional
    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new EntityNotFoundException("Task not found with id: " + id);
        }
        taskRepository.deleteById(id);
    }

    @Override
    @Transactional
    public TaskDTO cancelTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Task not found with id: " + id));

        if (!TaskStatus.PENDING.name().equals(task.getStatus()) &&
                !TaskStatus.RUNNING.name().equals(task.getStatus())) {
            throw new IllegalStateException("Only PENDING or RUNNING tasks can be cancelled");
        }

        task.setStatus(TaskStatus.CANCELLED.name());
        task.setFinishedAt(LocalDateTime.now());
        task = taskRepository.save(task);
        return TaskDTO.fromTask(task);
    }

    @Override
    @Transactional
    public TaskDTO completeTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Task not found with id: " + id));

        task.setStatus(TaskStatus.COMPLETED.name());
        task.setFinishedAt(LocalDateTime.now());
        task = taskRepository.save(task);
        return TaskDTO.fromTask(task);
    }

    @Override
    @Transactional
    public TaskLogDTO addTaskLog(Long taskId, TaskLogCreateRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found with id: " + taskId));

        TaskLog log = new TaskLog();
        log.setTask(task);
        log.setLevel(request.getLevel());
        log.setMessage(request.getMessage());

        log = taskLogRepository.save(log);
        return TaskLogDTO.fromTaskLog(log);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskLogDTO> getTaskLogs(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new EntityNotFoundException("Task not found with id: " + taskId);
        }
        return taskLogRepository.findByTaskId(taskId).stream()
                .map(TaskLogDTO::fromTaskLog)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void executeTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found with id: " + taskId));
        taskExecutionService.executeTask(taskId);
    }
}
