package com.bioinfo.platform.service;

import com.bioinfo.platform.entity.Task;
import com.bioinfo.platform.entity.TaskFile;
import com.bioinfo.platform.entity.TaskParameter;
import com.bioinfo.platform.entity.TaskResult;
import com.bioinfo.platform.repository.TaskFileRepository;
import com.bioinfo.platform.repository.TaskParameterRepository;
import com.bioinfo.platform.repository.TaskRepository;
import com.bioinfo.platform.repository.TaskResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskExecutionService {
    private final TaskRepository taskRepository;
    private final TaskParameterRepository taskParameterRepository;
    private final TaskFileRepository taskFileRepository;
    private final TaskResultRepository taskResultRepository;
    private final TaskLogService taskLogService;
    private final BioinformaticsAnalysisEngine analysisEngine;

    @Value("${bioinfo.upload.dir}")
    private String uploadDir;

    public Map<String, Object> getExecutionSolution(Long taskId) {
        Task task = taskRepository.findById(taskId).orElse(null);
        if (task == null) return Collections.emptyMap();
        String code = task.getAnalysisType().getCode();
        Map<String, String> params = parameterMap(taskId);
        Map<String, Object> solution = new LinkedHashMap<>();
        solution.put("taskId", taskId);
        solution.put("typeCode", code);
        solution.put("typeName", task.getAnalysisType().getName());
        solution.put("tool", "BioInfo Platform 内置分析引擎");
        solution.put("description", description(code));
        solution.put("parameters", params);
        solution.put("workflow", workflow(code));
        solution.put("command", "builtin://" + code + "/tasks/" + taskId);
        solution.put("reproducible", true);
        return solution;
    }

    @Async
    public void executeTask(Long taskId) {
        try {
            Task task = taskRepository.findById(taskId).orElseThrow();
            if (!"PENDING".equals(task.getStatus())) return;
            task.setStatus("RUNNING");
            task.setStartedAt(LocalDateTime.now());
            task.setFinishedAt(null);
            task.setErrorMessage(null);
            taskRepository.saveAndFlush(task);

            String code = task.getAnalysisType().getCode();
            List<TaskFile> inputFiles = taskFileRepository.findByTaskId(taskId);
            Map<String, String> params = parameterMap(taskId);
            Path output = Path.of(uploadDir, "tasks", String.valueOf(taskId), "results");

            taskLogService.info(taskId, "[1/5] 任务开始执行");
            taskLogService.info(taskId, "分析类型: " + task.getAnalysisType().getName() + " (" + code + ")");
            taskLogService.info(taskId, "[2/5] 校验输入文件，共 " + inputFiles.size() + " 个");
            for (TaskFile file : inputFiles) {
                if (!Files.isReadable(Path.of(file.getFilePath()))) throw new IllegalArgumentException("输入文件不可读: " + file.getOriginalName());
                taskLogService.info(taskId, "输入: " + file.getOriginalName() + " (" + file.getFileSize() + " bytes)");
            }

            taskLogService.info(taskId, "[3/5] 解析数据并执行分析");
            List<BioinformaticsAnalysisEngine.OutputFile> outputs = analysisEngine.run(
                    code, inputFiles, params, output, message -> taskLogService.info(taskId, message));

            if (isCancelled(taskId)) { taskLogService.warn(taskId, "检测到取消请求，停止保存结果"); return; }
            taskLogService.info(taskId, "[4/5] 保存分析结果");
            taskResultRepository.deleteByTaskId(taskId);
            for (BioinformaticsAnalysisEngine.OutputFile file : outputs) {
                TaskResult result = new TaskResult();
                result.setTask(task);
                result.setFileName(file.fileName());
                result.setFilePath(file.path().toAbsolutePath().toString());
                result.setFileSize(Files.size(file.path()));
                result.setFileType(extension(file.fileName()));
                result.setDescription(file.description());
                taskResultRepository.save(result);
            }

            task = taskRepository.findById(taskId).orElseThrow();
            if ("CANCELLED".equals(task.getStatus())) return;
            task.setOutputDir(output.toAbsolutePath().toString());
            task.setStatus("COMPLETED");
            task.setFinishedAt(LocalDateTime.now());
            taskRepository.save(task);
            taskLogService.info(taskId, "[5/5] 任务执行完成，共生成 " + outputs.size() + " 个结果文件");
        } catch (Exception e) {
            log.error("Task {} execution failed", taskId, e);
            taskRepository.findById(taskId).ifPresent(task -> {
                if (!"CANCELLED".equals(task.getStatus())) {
                    task.setStatus("FAILED");
                    task.setErrorMessage(rootMessage(e));
                    task.setFinishedAt(LocalDateTime.now());
                    taskRepository.save(task);
                    taskLogService.error(taskId, "任务执行失败: " + rootMessage(e));
                }
            });
        }
    }

    private Map<String, String> parameterMap(Long taskId) {
        return taskParameterRepository.findByTaskId(taskId).stream().collect(Collectors.toMap(
                TaskParameter::getParamName,
                p -> p.getParamValue() == null ? "" : p.getParamValue(),
                (left, right) -> right,
                LinkedHashMap::new));
    }

    private boolean isCancelled(Long taskId) {
        return taskRepository.findById(taskId).map(t -> "CANCELLED".equals(t.getStatus())).orElse(true);
    }

    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "FILE" : name.substring(dot + 1).toUpperCase();
    }

    private String rootMessage(Exception error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? error.getClass().getSimpleName() : current.getMessage();
    }

    private String description(String code) {
        return switch (code) {
            case "sequence_alignment" -> "将 FASTA/FASTQ reads 比对到首条参考序列，输出最佳位置、方向和 identity。";
            case "sequence_assembly" -> "使用贪心 overlap-layout 方法组装 reads，输出 contigs 与 N50 指标。";
            case "gene_expression" -> "解析表达矩阵并按样本库大小进行 CPM/TPM 风格标准化。";
            case "diff_expression" -> "计算组间均值、log2 fold change、近似显著性和 BH 校正。";
            case "variant_calling" -> "比较参考与样本序列，输出标准 VCF 4.3 SNP 记录。";
            case "functional_annotation" -> "根据序列类型、GC 含量和内置 motif 规则生成可解释注释。";
            default -> "执行内置分析流程。";
        };
    }

    private List<String> workflow(String code) {
        return switch (code) {
            case "sequence_alignment" -> List.of("读取参考和 reads", "正反链无缺口扫描", "计算 identity", "输出 TSV");
            case "sequence_assembly" -> List.of("读取 reads", "计算后缀/前缀 overlap", "迭代合并 contigs", "计算 N50");
            case "gene_expression" -> List.of("解析表达矩阵", "计算样本库大小", "标准化", "输出矩阵");
            case "diff_expression" -> List.of("识别样本分组", "计算组均值和 log2FC", "显著性估计", "BH 多重校正");
            case "variant_calling" -> List.of("读取参考与样本", "逐位比较", "识别 SNP", "输出 VCF");
            case "functional_annotation" -> List.of("识别序列类型", "统计序列特征", "匹配 motif", "输出注释表");
            default -> List.of("解析输入", "执行分析", "输出结果");
        };
    }
}
