package com.bioinfo.platform.controller;

import com.bioinfo.platform.dto.TaskResultDTO;
import com.bioinfo.platform.entity.TaskResult;
import com.bioinfo.platform.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(fileService.uploadFile(file));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("文件上传失败，请稍后重试");
        }
    }

    @PostMapping("/tasks/{taskId}/results")
    public ResponseEntity<TaskResultDTO> uploadTaskResult(@PathVariable Long taskId,
                                                          @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(fileService.uploadTaskResult(taskId, file));
    }

    @GetMapping("/tasks/{taskId}/results")
    public ResponseEntity<List<TaskResultDTO>> getTaskResults(@PathVariable Long taskId) {
        return ResponseEntity.ok(fileService.getTaskResults(taskId));
    }

    @GetMapping("/tasks/{taskId}/results/{resultId}/download")
    public ResponseEntity<Resource> downloadTaskResult(@PathVariable Long taskId,
                                                       @PathVariable Long resultId) throws IOException {
        TaskResult taskResult = fileService.getTaskResult(taskId, resultId);
        Resource resource = fileService.loadTaskResultAsResource(taskResult);
        String encodedFileName = URLEncoder.encode(taskResult.getFileName(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileService.detectContentType(taskResult)))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .body(resource);
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) throws IOException {
        Resource resource = fileService.loadStoredFileAsResource(filename);
        String encodedFileName = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileService.detectContentType(
                        Paths.get(resource.getFile().getAbsolutePath()))))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .body(resource);
    }
}
