-- ============================================================
-- Bioinformatics Analysis Task Management Platform
-- Database: bioinfo_platform
-- ============================================================

CREATE DATABASE IF NOT EXISTS `bioinfo_platform`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `bioinfo_platform`;

-- ============================================================
-- 1. analysis_type  - Analysis type lookup table
-- ============================================================
CREATE TABLE `analysis_type` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `name`           VARCHAR(100) NOT NULL COMMENT 'Analysis type display name',
  `code`           VARCHAR(50)  NOT NULL COMMENT 'Unique analysis type code',
  `description`    TEXT                      COMMENT 'Description of the analysis type',
  `default_params` JSON                     COMMENT 'Default parameters as JSON',
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_analysis_type_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Analysis type lookup table';

-- ============================================================
-- 2. task  - Main task table
-- ============================================================
CREATE TABLE `task` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT,
  `name`             VARCHAR(200) NOT NULL COMMENT 'Task name',
  `analysis_type_id` BIGINT       NOT NULL COMMENT 'FK to analysis_type',
  `status`           VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/COMPLETED/FAILED/CANCELLED',
  `priority`         VARCHAR(10)             DEFAULT 'MEDIUM' COMMENT 'LOW/MEDIUM/HIGH',
  `description`      TEXT                      COMMENT 'Task description',
  `input_files`      TEXT                      COMMENT 'Comma-separated input file paths',
  `output_dir`       VARCHAR(500)             COMMENT 'Output directory path',
  `error_message`    TEXT                      COMMENT 'Error message when task failed',
  `started_at`       DATETIME                 COMMENT 'Time the task started running',
  `finished_at`      DATETIME                 COMMENT 'Time the task finished',
  `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_task_status` (`status`),
  INDEX `idx_task_analysis_type_id` (`analysis_type_id`),
  INDEX `idx_task_created_at` (`created_at`),
  CONSTRAINT `fk_task_analysis_type`
    FOREIGN KEY (`analysis_type_id`) REFERENCES `analysis_type` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Main task table';

-- ============================================================
-- 3. task_parameter  - Task parameters
-- ============================================================
CREATE TABLE `task_parameter` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `task_id`    BIGINT       NOT NULL COMMENT 'FK to task',
  `param_name` VARCHAR(100) NOT NULL COMMENT 'Parameter name',
  `param_value` TEXT                    COMMENT 'Parameter value',
  `param_type` VARCHAR(20)             DEFAULT 'STRING' COMMENT 'STRING/NUMBER/BOOLEAN/FILE',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_task_parameter_task_id` (`task_id`),
  CONSTRAINT `fk_task_parameter_task`
    FOREIGN KEY (`task_id`) REFERENCES `task` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Task parameters';

-- ============================================================
-- 4. task_log  - Task execution logs
-- ============================================================
CREATE TABLE `task_log` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `task_id`    BIGINT       NOT NULL COMMENT 'FK to task',
  `level`      VARCHAR(10)  NOT NULL COMMENT 'INFO/WARN/ERROR',
  `message`    TEXT         NOT NULL COMMENT 'Log message',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_task_log_task_id` (`task_id`),
  INDEX `idx_task_log_level` (`level`),
  CONSTRAINT `fk_task_log_task`
    FOREIGN KEY (`task_id`) REFERENCES `task` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Task execution logs';

-- ============================================================
-- 5. task_result  - Task result files
-- ============================================================
CREATE TABLE `task_result` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `task_id`     BIGINT       NOT NULL COMMENT 'FK to task',
  `file_name`   VARCHAR(255) NOT NULL COMMENT 'Result file name',
  `file_path`   VARCHAR(500) NOT NULL COMMENT 'Result file path',
  `file_size`   BIGINT                  COMMENT 'File size in bytes',
  `file_type`   VARCHAR(50)             COMMENT 'File type / extension',
  `description` TEXT                     COMMENT 'Description of the result file',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_task_result_task_id` (`task_id`),
  CONSTRAINT `fk_task_result_task`
    FOREIGN KEY (`task_id`) REFERENCES `task` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Task result files';

-- ============================================================
-- Seed data: analysis_type
-- ============================================================
INSERT INTO `analysis_type` (`name`, `code`, `description`, `default_params`) VALUES
(
  '序列比对',
  'sequence_alignment',
  '将测序reads比对到参考基因组，常用工具包括 BWA、Bowtie2、HISAT2 等',
  '{"tool":"bwa","reference":"","threads":4,"read_group":"","output_format":"BAM"}'
),
(
  '序列组装',
  'sequence_assembly',
  '从测序reads拼接重建完整序列，常用工具包括 SPAdes、MEGAHIT、Canu 等',
  '{"tool":"spades","coverage_cutoff":200,"threads":4,"careful":true,"expected_coverage":""}'
),
(
  '基因表达分析',
  'gene_expression',
  '定量基因表达水平，常用工具包括 STAR+featureCounts、HISAT2+StringTie 等',
  '{"aligner":"star","quantifier":"featurecounts","reference":"","annotation":"","threads":4,"strand_mode":"unstranded"}'
),
(
  '差异表达分析',
  'diff_expression',
  '比较不同条件间的基因表达差异，常用工具包括 DESeq2、edgeR、limma 等',
  '{"tool":"deseq2","contrast":"","padj_cutoff":0.05,"lfc_cutoff":1.0,"normalization":"median","plot_pca":true}'
),
(
  '变异检测',
  'variant_calling',
  '检测基因组中的SNP和InDel变异，常用工具包括 GATK HaplotypeCaller、FreeBayes 等',
  '{"tool":"gatk","reference":"","known_sites":"","interval_list":"","emit_ref_confidence":"GVCF","threads":4}'
),
(
  '功能注释',
  'functional_annotation',
  '对基因或蛋白质进行功能注释与富集分析，常用工具包括 BLAST、InterProScan、clusterProfiler 等',
  '{"tool":"blast","database":"","evalue":1e-5,"outfmt":6,"threads":4,"gene_ontology":true,"kegg":true}'
);
