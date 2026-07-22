package com.example.biotaskplatform.entity;

import lombok.Data;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
@Data
@TableName("analysis_task")
public class AnalysisTask{

    /**
     * 任务ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 分析类型ID
     */
    private Long taskTypeId;

    /**
     * 分析参数
     */
    private String parameter;

    /**
     * 任务状态
     */
    private String status;

    /**
     * 创建人
     */
    private String createUser;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}