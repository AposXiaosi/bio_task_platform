package com.bioinfo.platform.service;

import com.bioinfo.platform.entity.Task;
import com.bioinfo.platform.entity.TaskLog;
import com.bioinfo.platform.entity.TaskResult;
import com.bioinfo.platform.enums.LogLevel;
import com.bioinfo.platform.enums.TaskStatus;
import com.bioinfo.platform.repository.TaskLogRepository;
import com.bioinfo.platform.repository.TaskResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SequenceAlignmentService {

    private static final long MAX_ALIGNMENT_MATRIX_CELLS = 4_000_000L;

    private final TaskLogRepository taskLogRepository;
    private final TaskResultRepository taskResultRepository;

    @Value("${bioinfo.upload.dir}")
    private String uploadDir;

    public void execute(Task task, Map<String, String> parameters) throws IOException {
        String referenceGenomePath = parameters != null ? parameters.get("referenceGenome") : null;
        if (!StringUtils.hasText(referenceGenomePath)) {
            throw new IllegalArgumentException("序列比对缺少参考基因组路径 referenceGenome");
        }
        if (!StringUtils.hasText(task.getInputFiles())) {
            throw new IllegalArgumentException("序列比对缺少输入文件路径");
        }

        Path outputDirectory = prepareOutputDirectory(task.getId());
        task.setOutputDir(outputDirectory.toString());
        task.setStatus(TaskStatus.RUNNING.name());
        task.setStartedAt(LocalDateTime.now());
        task.setFinishedAt(null);
        task.setErrorMessage(null);

        int matchScore = parsePositiveInt(parameters != null ? parameters.get("matchScore") : null, 2);
        int mismatchPenalty = parsePositiveInt(parameters != null ? parameters.get("mismatchPenalty") : null, 1);
        int gapPenalty = parsePositiveInt(parameters != null ? parameters.get("gapPenalty") : null, 2);

        log(task, LogLevel.INFO.name(), "开始执行序列比对任务");

        Path referencePath = resolveInputPath(referenceGenomePath);
        String referenceSequence = readSequence(referencePath);
        log(task, LogLevel.INFO.name(), "已加载参考序列: " + referencePath.getFileName());

        List<String> inputFilePaths = parseInputFiles(task.getInputFiles());
        int resultCount = 0;

        for (String inputFilePath : inputFilePaths) {
            Path queryPath = resolveInputPath(inputFilePath);
            String querySequence = readSequence(queryPath);
            log(task, LogLevel.INFO.name(), "正在比对输入序列: " + queryPath.getFileName());

            AlignmentResult alignmentResult = align(referenceSequence, querySequence, matchScore, mismatchPenalty, gapPenalty);
            Path resultFile = writeAlignmentResult(outputDirectory, queryPath, referencePath, alignmentResult);

            TaskResult taskResult = new TaskResult();
            taskResult.setTask(task);
            taskResult.setFileName(resultFile.getFileName().toString());
            taskResult.setFilePath(resultFile.toString());
            taskResult.setFileSize(Files.size(resultFile));
            taskResult.setFileType(".txt");
            taskResult.setDescription("序列比对结果文件");
            taskResultRepository.save(taskResult);
            task.getResults().add(taskResult);

            resultCount++;
            log(task, LogLevel.INFO.name(), "已生成比对结果文件: " + resultFile.getFileName());
        }

        task.setStatus(TaskStatus.COMPLETED.name());
        task.setFinishedAt(LocalDateTime.now());
        log(task, LogLevel.INFO.name(), "序列比对完成，共生成 " + resultCount + " 个结果文件");
    }

    private Path prepareOutputDirectory(Long taskId) throws IOException {
        Path outputDirectory = Paths.get(uploadDir, "analysis-results", "task-" + taskId).toAbsolutePath().normalize();
        Files.createDirectories(outputDirectory);
        return outputDirectory;
    }

    private Path resolveInputPath(String rawPath) {
        String normalizedPath = sanitizePath(rawPath);
        Path candidate = Paths.get(normalizedPath).toAbsolutePath().normalize();
        if (Files.exists(candidate)) {
            return candidate;
        }

        Path uploadCandidate = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(normalizedPath).normalize();
        if (Files.exists(uploadCandidate)) {
            return uploadCandidate;
        }

        throw new IllegalArgumentException("找不到输入文件: " + normalizedPath);
    }

    private List<String> parseInputFiles(String inputFiles) {
        List<String> result = new ArrayList<>();
        for (String item : inputFiles.split(",")) {
            String value = sanitizePath(item);
            if (StringUtils.hasText(value)) {
                result.add(value);
            }
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("至少需要一个输入序列文件");
        }
        return result;
    }

    private String sanitizePath(String rawPath) {
        if (!StringUtils.hasText(rawPath)) {
            return rawPath;
        }

        String value = rawPath.trim();
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1).trim();
        }
        return value;
    }

    private int parsePositiveInt(String value, int defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String readSequence(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        StringBuilder sequenceBuilder = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!StringUtils.hasText(trimmed) || trimmed.startsWith(">") || trimmed.startsWith(";")) {
                continue;
            }
            for (char ch : trimmed.toUpperCase(Locale.ROOT).toCharArray()) {
                if (Character.isLetter(ch)) {
                    sequenceBuilder.append(ch);
                }
            }
        }
        String sequence = sequenceBuilder.toString();
        if (!StringUtils.hasText(sequence)) {
            throw new IllegalArgumentException("文件中未读取到有效序列: " + path.getFileName());
        }
        return sequence;
    }

    private AlignmentResult align(String referenceSequence, String querySequence, int matchScore, int mismatchPenalty, int gapPenalty) {
        long cellCount = (long) (referenceSequence.length() + 1) * (querySequence.length() + 1);
        if (cellCount > MAX_ALIGNMENT_MATRIX_CELLS) {
            throw new IllegalArgumentException("序列过长，当前演示版仅支持中小规模序列比对");
        }

        int rows = referenceSequence.length() + 1;
        int cols = querySequence.length() + 1;
        int[][] scores = new int[rows][cols];
        byte[][] directions = new byte[rows][cols];

        for (int i = 1; i < rows; i++) {
            scores[i][0] = scores[i - 1][0] - gapPenalty;
            directions[i][0] = 2;
        }
        for (int j = 1; j < cols; j++) {
            scores[0][j] = scores[0][j - 1] - gapPenalty;
            directions[0][j] = 3;
        }

        for (int i = 1; i < rows; i++) {
            for (int j = 1; j < cols; j++) {
                int diagonal = scores[i - 1][j - 1]
                        + (referenceSequence.charAt(i - 1) == querySequence.charAt(j - 1) ? matchScore : -mismatchPenalty);
                int up = scores[i - 1][j] - gapPenalty;
                int left = scores[i][j - 1] - gapPenalty;

                int best = diagonal;
                byte direction = 1;
                if (up > best) {
                    best = up;
                    direction = 2;
                }
                if (left > best) {
                    best = left;
                    direction = 3;
                }

                scores[i][j] = best;
                directions[i][j] = direction;
            }
        }

        StringBuilder alignedReference = new StringBuilder();
        StringBuilder alignedQuery = new StringBuilder();
        int i = referenceSequence.length();
        int j = querySequence.length();

        while (i > 0 || j > 0) {
            byte direction = directions[i][j];
            if (i > 0 && j > 0 && direction == 1) {
                alignedReference.append(referenceSequence.charAt(i - 1));
                alignedQuery.append(querySequence.charAt(j - 1));
                i--;
                j--;
            } else if (i > 0 && (direction == 2 || j == 0)) {
                alignedReference.append(referenceSequence.charAt(i - 1));
                alignedQuery.append('-');
                i--;
            } else {
                alignedReference.append('-');
                alignedQuery.append(querySequence.charAt(j - 1));
                j--;
            }
        }

        String finalReference = alignedReference.reverse().toString();
        String finalQuery = alignedQuery.reverse().toString();
        int matches = 0;
        int mismatches = 0;
        int gaps = 0;

        for (int index = 0; index < finalReference.length(); index++) {
            char refChar = finalReference.charAt(index);
            char queryChar = finalQuery.charAt(index);
            if (refChar == '-' || queryChar == '-') {
                gaps++;
            } else if (refChar == queryChar) {
                matches++;
            } else {
                mismatches++;
            }
        }

        double identity = finalReference.isEmpty() ? 0D : (matches * 100.0D / finalReference.length());
        return new AlignmentResult(scores[referenceSequence.length()][querySequence.length()], matches, mismatches, gaps, identity, finalReference, finalQuery);
    }

    private Path writeAlignmentResult(Path outputDirectory, Path queryPath, Path referencePath, AlignmentResult alignmentResult) throws IOException {
        String baseName = stripExtension(queryPath.getFileName().toString());
        Path resultFile = outputDirectory.resolve(baseName + "_alignment_result.txt");

        StringBuilder content = new StringBuilder();
        content.append("序列比对结果").append(System.lineSeparator());
        content.append("====================").append(System.lineSeparator());
        content.append("参考序列文件: ").append(referencePath.getFileName()).append(System.lineSeparator());
        content.append("输入序列文件: ").append(queryPath.getFileName()).append(System.lineSeparator());
        content.append("比对得分: ").append(alignmentResult.score()).append(System.lineSeparator());
        content.append("匹配数: ").append(alignmentResult.matches()).append(System.lineSeparator());
        content.append("错配数: ").append(alignmentResult.mismatches()).append(System.lineSeparator());
        content.append("Gap 数: ").append(alignmentResult.gaps()).append(System.lineSeparator());
        content.append(String.format(Locale.ROOT, "序列一致性: %.2f%%", alignmentResult.identity())).append(System.lineSeparator());
        content.append(System.lineSeparator());
        content.append("参考序列比对结果:").append(System.lineSeparator());
        content.append(alignmentResult.alignedReference()).append(System.lineSeparator());
        content.append(System.lineSeparator());
        content.append("输入序列比对结果:").append(System.lineSeparator());
        content.append(alignmentResult.alignedQuery()).append(System.lineSeparator());

        Files.writeString(
                resultFile,
                content.toString(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
        return resultFile;
    }

    private String stripExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    private void log(Task task, String level, String message) {
        TaskLog taskLog = new TaskLog();
        taskLog.setTask(task);
        taskLog.setLevel(level);
        taskLog.setMessage(message);
        taskLogRepository.save(taskLog);
        task.getLogs().add(taskLog);
    }

    private record AlignmentResult(
            int score,
            int matches,
            int mismatches,
            int gaps,
            double identity,
            String alignedReference,
            String alignedQuery
    ) {
    }
}
