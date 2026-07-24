package com.bioinfo.platform.dto;

import lombok.Data;

@Data
public class FileUploadResponse {
    private Long fileId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String fileType;
}
