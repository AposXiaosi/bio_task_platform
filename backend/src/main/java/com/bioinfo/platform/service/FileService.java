package com.bioinfo.platform.service;

import com.bioinfo.platform.dto.TaskResultDTO;
import com.bioinfo.platform.entity.TaskResult;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface FileService {

    String uploadFile(MultipartFile file) throws IOException;

    TaskResultDTO uploadTaskResult(Long taskId, MultipartFile file) throws IOException;

    List<TaskResultDTO> getTaskResults(Long taskId);

    TaskResult getTaskResult(Long taskId, Long resultId);

    Resource loadTaskResultAsResource(TaskResult taskResult) throws IOException;

    Resource loadStoredFileAsResource(String filename) throws IOException;

    String detectContentType(TaskResult taskResult) throws IOException;

    String detectContentType(Path filePath) throws IOException;
}
