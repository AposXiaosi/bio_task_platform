DROP TABLE IF EXISTS analysis_task;

CREATE TABLE analysis_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',

    task_name VARCHAR(100) NOT NULL COMMENT '任务名称',

    task_type_id BIGINT NOT NULL COMMENT '分析类型ID',

    parameter TEXT COMMENT '分析参数(JSON)',

    status VARCHAR(20) NOT NULL DEFAULT 'WAITING' COMMENT '任务状态',

    create_user VARCHAR(50) COMMENT '创建人',

    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_task_type(task_type_id),

    INDEX idx_status(status)
) COMMENT='分析任务表';