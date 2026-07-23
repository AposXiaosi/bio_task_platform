package com.bioinfo.platform.controller;

import com.bioinfo.platform.dto.FileUploadResponse;
import com.bioinfo.platform.dto.TaskFileDTO;
import com.bioinfo.platform.dto.TaskResultDTO;
import com.bioinfo.platform.entity.TaskFile;
import com.bioinfo.platform.entity.TaskResult;
import com.bioinfo.platform.repository.TaskFileRepository;
import com.bioinfo.platform.repository.TaskResultRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final TaskResultRepository taskResultRepository;
    private final TaskFileRepository taskFileRepository;

    @Value("${bioinfo.upload.dir}")
    private String uploadDir;

    /**
     * 上传文件接口：保存文件到磁盘，并在 DB 中创建 TaskFile 记录（task_id 初始为 null）
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "文件为空"));
        }

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String storedFilename = UUID.randomUUID() + extension;

            Path filePath = uploadPath.resolve(storedFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 推断文件类型
            String fileType = extension.isEmpty() ? "unknown" : extension.substring(1).toUpperCase();

            // 保存到 TaskFile 表（task_id 为 null，任务创建时再关联）
            TaskFile taskFile = new TaskFile();
            taskFile.setOriginalName(originalFilename);
            taskFile.setFileName(storedFilename);
            taskFile.setFilePath(filePath.toString());
            taskFile.setFileSize(file.getSize());
            taskFile.setFileType(fileType);
            taskFile = taskFileRepository.save(taskFile);

            FileUploadResponse data = new FileUploadResponse();
            data.setFileId(taskFile.getId());
            data.setFileName(originalFilename);
            data.setFilePath(filePath.toString());
            data.setFileSize(file.getSize());
            data.setFileType(fileType);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "上传成功");
            response.put("data", data);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(
                Map.of("code", 500, "message", "文件上传失败: " + e.getMessage())
            );
        }
    }

    /**
     * 根据文件 ID 下载文件
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        TaskFile taskFile = taskFileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("文件不存在，ID: " + id));

        try {
            Path filePath = Paths.get(taskFile.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + taskFile.getOriginalName() + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取任务的输入文件列表
     */
    @GetMapping("/tasks/{taskId}/files")
    public ResponseEntity<List<TaskFileDTO>> getTaskFiles(@PathVariable Long taskId) {
        List<TaskFile> files = taskFileRepository.findByTaskId(taskId);
        List<TaskFileDTO> dtos = files.stream()
                .map(TaskFileDTO::fromTaskFile)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * 获取任务的结果文件列表（保留原有接口）
     */
    @GetMapping("/tasks/{taskId}/results")
    public ResponseEntity<List<TaskResultDTO>> getTaskResults(@PathVariable Long taskId) {
        List<TaskResult> results = taskResultRepository.findByTaskId(taskId);
        List<TaskResultDTO> dtos = results.stream()
                .map(TaskResultDTO::fromTaskResult)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}
