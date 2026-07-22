package com.bioinfo.platform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Data
@Entity
@Table(name = "task_parameter")
public class TaskParameter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    @JsonIgnore
    @ToString.Exclude
    private Task task;

    @Column(name = "param_name", nullable = false)
    private String paramName;

    @Column(name = "param_value", columnDefinition = "TEXT")
    private String paramValue;

    @Column(name = "param_type")
    private String paramType;
}
