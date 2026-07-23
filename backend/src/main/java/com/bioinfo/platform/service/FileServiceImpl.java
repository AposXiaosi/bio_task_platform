package com.bioinfo.platform.service;

import com.bioinfo.platform.dto.TaskResultDTO;
import com.bioinfo.platform.entity.Task;
import com.bioinfo.platform.entity.TaskResult;
import com.bioinfo.platform.repository.TaskRepository;
import com.bioinfo.platform.repository.TaskResultRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".txt", ".fasta", ".fa");

    private final TaskRepository taskRepository;
    private final TaskResultRepository taskResultRepository;

    @Value("${bioinfo.upload.dir}")
    private String uploadDir;

    @Value("${spring.servlet.multipart.max-file-size:100MB}")
    private DataSize maxFileSize;

    @Override
    public String uploadFile(MultipartFile file) throws IOException {
        validateFile(file);
        String originalFilename = sanitizeOriginalFilename(file.getOriginalFilename());
        Path uploadPath = getUploadPath();
        String storedFilename = buildStoredFilename(originalFilename);
        Path targetFile = uploadPath.resolve(storedFilename);
        Files.copy(file.getInputStream(), targetFile);
        return targetFile.toString();
    }

    @Override
    public TaskResultDTO uploadTaskResult(Long taskId, MultipartFile file) throws IOException {
        if (taskId == null) {
            throw new IllegalArgumentException("taskId 不能为空");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("任务不存在，taskId=" + taskId));

        validateFile(file);
        String originalFilename = sanitizeOriginalFilename(file.getOriginalFilename());
        Path uploadPath = getUploadPath();
        String storedFilename = buildStoredFilename(originalFilename);
        Path targetFile = uploadPath.resolve(storedFilename);
        Files.copy(file.getInputStream(), targetFile);

        TaskResult taskResult = new TaskResult();
        taskResult.setTask(task);
        taskResult.setFileName(originalFilename);
        taskResult.setFilePath(targetFile.toString());
        taskResult.setFileSize(file.getSize());
        taskResult.setFileType(getExtension(originalFilename));
        taskResult.setDescription("任务结果文件");

        return TaskResultDTO.fromTaskResult(taskResultRepository.save(taskResult));
    }

    @Override
    public List<TaskResultDTO> getTaskResults(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new EntityNotFoundException("任务不存在，taskId=" + taskId);
        }
        return taskResultRepository.findByTaskIdOrderByCreatedAtDesc(taskId).stream()
                .map(TaskResultDTO::fromTaskResult)
                .collect(Collectors.toList());
    }

    @Override
    public TaskResult getTaskResult(Long taskId, Long resultId) {
        if (!taskRepository.existsById(taskId)) {
            throw new EntityNotFoundException("任务不存在，taskId=" + taskId);
        }
        return taskResultRepository.findByIdAndTaskId(resultId, taskId)
                .orElseThrow(() -> new EntityNotFoundException("结果文件不存在，resultId=" + resultId));
    }

    @Override
    public Resource loadTaskResultAsResource(TaskResult taskResult) throws IOException {
        Path uploadPath = getUploadPath();
        Path normalizedFilePath = Paths.get(taskResult.getFilePath()).toAbsolutePath().normalize();
        if (!normalizedFilePath.startsWith(uploadPath)) {
            throw new IllegalArgumentException("文件路径不合法，禁止非法访问");
        }

        try {
            Resource resource = new UrlResource(normalizedFilePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new EntityNotFoundException("文件不存在或无法读取");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("文件路径不合法");
        }
    }

    @Override
    public Resource loadStoredFileAsResource(String filename) throws IOException {
        if (!StringUtils.hasText(filename)) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String safeFilename = sanitizeStoredFilename(filename);
        Path uploadPath = getUploadPath();
        Path filePath = uploadPath.resolve(safeFilename).normalize();
        if (!filePath.startsWith(uploadPath)) {
            throw new IllegalArgumentException("文件路径不合法，禁止非法访问");
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new EntityNotFoundException("文件不存在或无法读取");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("文件路径不合法");
        }
    }

    @Override
    public String detectContentType(TaskResult taskResult) throws IOException {
        Path normalizedFilePath = Paths.get(taskResult.getFilePath()).toAbsolutePath().normalize();
        String contentType = Files.probeContentType(normalizedFilePath);
        return contentType != null ? contentType : "application/octet-stream";
    }

    @Override
    public String detectContentType(Path filePath) throws IOException {
        String contentType = Files.probeContentType(filePath);
        return contentType != null ? contentType : "application/octet-stream";
    }

    private Path getUploadPath() throws IOException {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);
        return uploadPath;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的文件，禁止空文件上传");
        }
        if (file.getSize() > maxFileSize.toBytes()) {
            throw new IllegalArgumentException("上传文件过大，不能超过" + formatFileSize(maxFileSize));
        }
        String originalFilename = sanitizeOriginalFilename(file.getOriginalFilename());
        validateExtension(originalFilename);
    }

    private String sanitizeOriginalFilename(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            throw new IllegalArgumentException("文件名不能为空，请重新选择文件");
        }

        String cleanedFilename = StringUtils.cleanPath(originalFilename).trim();
        if (!StringUtils.hasText(cleanedFilename)) {
            throw new IllegalArgumentException("文件名不能为空，请重新选择文件");
        }
        if (cleanedFilename.contains("..")) {
            throw new IllegalArgumentException("文件名不合法，禁止包含路径穿越内容");
        }

        String fileNameOnly = Paths.get(cleanedFilename).getFileName().toString();
        if (!StringUtils.hasText(fileNameOnly) || !cleanedFilename.equals(fileNameOnly)) {
            throw new IllegalArgumentException("文件名不合法，请重新选择文件");
        }

        return fileNameOnly;
    }

    private void validateExtension(String originalFilename) {
        String extension = getExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("文件格式不支持，仅允许上传 .txt、.fasta、.fa 文件");
        }
    }

    private String getExtension(String originalFilename) {
        int dotIndex = originalFilename.lastIndexOf(".");
        if (dotIndex <= 0) {
            throw new IllegalArgumentException("文件格式不支持，仅允许上传 .txt、.fasta、.fa 文件");
        }
        return originalFilename.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private String buildStoredFilename(String originalFilename) {
        return UUID.randomUUID().toString().replace("-", "") + getExtension(originalFilename);
    }

    private String sanitizeStoredFilename(String filename) {
        String cleanedFilename = StringUtils.cleanPath(filename).trim();
        if (!StringUtils.hasText(cleanedFilename) || cleanedFilename.contains("..")) {
            throw new IllegalArgumentException("文件名不合法");
        }
        String fileNameOnly = Paths.get(cleanedFilename).getFileName().toString();
        if (!cleanedFilename.equals(fileNameOnly)) {
            throw new IllegalArgumentException("文件名不合法");
        }
        return fileNameOnly;
    }

    private String formatFileSize(DataSize dataSize) {
        long bytes = dataSize.toBytes();
        long mb = 1024L * 1024L;
        long kb = 1024L;
        if (bytes % mb == 0) {
            return bytes / mb + "MB";
        }
        if (bytes % kb == 0) {
            return bytes / kb + "KB";
        }
        return bytes + "B";
    }
}
