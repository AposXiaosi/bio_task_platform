package com.bioinfo.platform.controller;

import com.bioinfo.platform.dto.*;
import com.bioinfo.platform.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskDTO> createTask(@Valid @RequestBody TaskCreateRequest request) {
        TaskDTO taskDTO = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(taskDTO);
    }

    @GetMapping
    public ResponseEntity<PageResponse<TaskDTO>> getTaskList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long analysisTypeId,
            @RequestParam(required = false) String keyword) {
        PageResponse<TaskDTO> response = taskService.getTaskList(page, size, status, analysisTypeId, keyword);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDTO> getTaskById(@PathVariable Long id) {
        TaskDTO taskDTO = taskService.getTaskById(id);
        return ResponseEntity.ok(taskDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskDTO> updateTask(@PathVariable Long id, @Valid @RequestBody TaskUpdateRequest request) {
        TaskDTO taskDTO = taskService.updateTask(id, request);
        return ResponseEntity.ok(taskDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<TaskDTO> cancelTask(@PathVariable Long id) {
        TaskDTO taskDTO = taskService.cancelTask(id);
        return ResponseEntity.ok(taskDTO);
    }

    @PostMapping("/{id}/logs")
    public ResponseEntity<TaskLogDTO> addTaskLog(@PathVariable Long id, @Valid @RequestBody TaskLogCreateRequest request) {
        TaskLogDTO logDTO = taskService.addTaskLog(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(logDTO);
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<List<TaskLogDTO>> getTaskLogs(@PathVariable Long id) {
        List<TaskLogDTO> logs = taskService.getTaskLogs(id);
        return ResponseEntity.ok(logs);
    }
}
