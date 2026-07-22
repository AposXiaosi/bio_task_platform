package com.bioinfo.platform.dto;

import lombok.Data;

@Data
public class TaskUpdateRequest {
    private String name;
    private String description;
    private String priority;
}
