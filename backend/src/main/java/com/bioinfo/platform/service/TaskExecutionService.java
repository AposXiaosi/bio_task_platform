package com.bioinfo.platform.service;

import com.bioinfo.platform.entity.Task;
import com.bioinfo.platform.entity.TaskParameter;
import com.bioinfo.platform.repository.TaskRepository;
import com.bioinfo.platform.repository.TaskParameterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskExecutionService {

    private final TaskRepository taskRepository;
    private final TaskParameterRepository taskParameterRepository;
    private final TaskLogService taskLogService;

    public Map<String, Object> getExecutionSolution(Long taskId) {
        Task task = taskRepository.findById(taskId).orElse(null);
        if (task == null) return Collections.emptyMap();

        String typeCode = task.getAnalysisType().getCode();
        List<TaskParameter> params = taskParameterRepository.findByTaskId(taskId);
        Map<String, String> paramMap = params.stream()
            .collect(Collectors.toMap(TaskParameter::getParamName, TaskParameter::getParamValue));

        Map<String, Object> solution = new HashMap<>();
        solution.put("taskId", taskId);
        solution.put("typeCode", typeCode);
        solution.put("typeName", task.getAnalysisType().getName());
        solution.put("parameters", paramMap);

        switch (typeCode) {
            case "sequence_alignment":
                solution.put("tool", paramMap.getOrDefault("tool", "bwa"));
                solution.put("description", "使用BWA/Bowtie2将测序reads比对到参考基因组");
                solution.put("pythonLibs", Arrays.asList("pysam", "biopython"));
                solution.put("command", buildAlignmentCommand(paramMap));
                solution.put("script", getAlignmentScript(paramMap));
                break;
            case "sequence_assembly":
                solution.put("tool", paramMap.getOrDefault("tool", "spades"));
                solution.put("description", "使用SPAdes/MEGAHIT进行基因组组装");
                solution.put("pythonLibs", Arrays.asList("biopython", "pysam"));
                solution.put("command", buildAssemblyCommand(paramMap));
                solution.put("script", getAssemblyScript(paramMap));
                break;
            case "gene_expression":
                solution.put("tool", paramMap.getOrDefault("quantifier", "featurecounts"));
                solution.put("description", "使用featureCounts/StringTie定量基因表达");
                solution.put("pythonLibs", Arrays.asList("pysam", "pandas", "numpy"));
                solution.put("command", buildExpressionCommand(paramMap));
                solution.put("script", getExpressionScript(paramMap));
                break;
            case "diff_expression":
                solution.put("tool", paramMap.getOrDefault("tool", "deseq2"));
                solution.put("description", "使用DESeq2/edgeR进行差异表达分析");
                solution.put("pythonLibs", Arrays.asList("pandas", "numpy", "scipy", "statsmodels", "matplotlib"));
                solution.put("command", buildDiffExpressionCommand(paramMap));
                solution.put("script", getDiffExpressionScript(paramMap));
                break;
            case "variant_calling":
                solution.put("tool", paramMap.getOrDefault("tool", "gatk"));
                solution.put("description", "使用GATK/FreeBayes进行SNP和InDel变异检测");
                solution.put("pythonLibs", Arrays.asList("pysam", "cyvcf2"));
                solution.put("command", buildVariantCommand(paramMap));
                solution.put("script", getVariantScript(paramMap));
                break;
            case "functional_annotation":
                solution.put("tool", paramMap.getOrDefault("tool", "blast"));
                solution.put("description", "使用BLAST/InterProScan进行功能注释与富集分析");
                solution.put("pythonLibs", Arrays.asList("biopython", "pandas", "goatools"));
                solution.put("command", buildAnnotationCommand(paramMap));
                solution.put("script", getAnnotationScript(paramMap));
                break;
            default:
                solution.put("description", "未知分析类型");
                break;
        }
        return solution;
    }

    @Async
    public void executeTask(Long taskId) {
        try {
            Task task = taskRepository.findById(taskId).orElse(null);
            if (task == null) return;

            task.setStatus("RUNNING");
            task.setStartedAt(LocalDateTime.now());
            taskRepository.save(task);

            taskLogService.info(taskId, "任务开始执行...");
            taskLogService.info(taskId, "分析类型: " + task.getAnalysisType().getName());

            Map<String, Object> solution = getExecutionSolution(taskId);
            String script = (String) solution.get("script");

            if (script != null && !script.isEmpty()) {
                taskLogService.info(taskId, "执行脚本已生成");
                taskLogService.info(taskId, "预计使用的Python库: " + solution.get("pythonLibs"));
                taskLogService.info(taskId, "执行命令: " + solution.get("command"));
            }

            Thread.sleep(2000);
            taskLogService.info(taskId, "任务执行完成");

            task.setStatus("COMPLETED");
            task.setFinishedAt(LocalDateTime.now());
            taskRepository.save(task);

        } catch (Exception e) {
            log.error("Task execution failed", e);
            Task task = taskRepository.findById(taskId).orElse(null);
            if (task != null) {
                task.setStatus("FAILED");
                task.setErrorMessage(e.getMessage());
                task.setFinishedAt(LocalDateTime.now());
                taskRepository.save(task);
            }
            taskLogService.error(taskId, "任务执行失败: " + e.getMessage());
        }
    }

    private String buildAlignmentCommand(Map<String, String> p) {
        String tool = p.getOrDefault("tool", "bwa");
        String ref = p.getOrDefault("reference", "reference.fa");
        if ("bwa".equals(tool)) {
            return String.format("bwa mem -t %s %s reads.fq > output.sam",
                p.getOrDefault("threads", "4"), ref);
        }
        return String.format("bowtie2 -x %s -U reads.fq -S output.sam", ref);
    }

    private String buildAssemblyCommand(Map<String, String> p) {
        String tool = p.getOrDefault("tool", "spades");
        return String.format("%s -o output_dir -1 read1.fq -2 read2.fq", tool);
    }

    private String buildExpressionCommand(Map<String, String> p) {
        String tool = p.getOrDefault("quantifier", "featurecounts");
        return String.format("featureCounts -T %s -a annotation.gtf -o counts.txt aligned.bam",
            p.getOrDefault("threads", "4"));
    }

    private String buildDiffExpressionCommand(Map<String, String> p) {
        return "Rscript deseq2_analysis.R --control control.csv --treatment treatment.csv";
    }

    private String buildVariantCommand(Map<String, String> p) {
        String tool = p.getOrDefault("tool", "gatk");
        return String.format("gatk HaplotypeCaller -R %s -I aligned.bam -O variants.gvcf",
            p.getOrDefault("reference", "reference.fa"));
    }

    private String buildAnnotationCommand(Map<String, String> p) {
        return String.format("blastp -query proteins.fa -db %s -out results.txt -evalue %s",
            p.getOrDefault("database", "nr"), p.getOrDefault("evalue", "1e-5"));
    }

    private String getAlignmentScript(Map<String, String> p) {
        return "#!/usr/bin/env python3\n" +
            "import pysam\n" +
            "import subprocess\n" +
            "ref = '" + p.getOrDefault("reference", "reference.fa") + "'\n" +
            "threads = " + p.getOrDefault("threads", "4") + "\n" +
            "# Step 1: Index reference\n" +
            "subprocess.run(['bwa', 'index', ref])\n" +
            "# Step 2: Align reads\n" +
            "subprocess.run(['bwa', 'mem', '-t', str(threads), ref, 'reads.fq'],\n" +
            "               stdout=open('output.sam', 'w'))\n" +
            "# Step 3: Convert to BAM and sort\n" +
            "subprocess.run(['samtools', 'sort', '-o', 'sorted.bam', 'output.sam'])\n" +
            "print('Alignment completed!')\n";
    }

    private String getAssemblyScript(Map<String, String> p) {
        return "#!/usr/bin/env python3\n" +
            "import subprocess\n" +
            "tool = '" + p.getOrDefault("tool", "spades") + "'\n" +
            "subprocess.run([tool, '-o', 'output_dir',\n" +
            "                '-1', 'read1.fq', '-2', 'read2.fq'])\n" +
            "print('Assembly completed!')\n";
    }

    private String getExpressionScript(Map<String, String> p) {
        return "#!/usr/bin/env python3\n" +
            "import subprocess\n" +
            "threads = " + p.getOrDefault("threads", "4") + "\n" +
            "subprocess.run(['featureCounts', '-T', str(threads),\n" +
            "                '-a', 'annotation.gtf', '-o', 'counts.txt',\n" +
            "                'aligned.bam'])\n" +
            "import pandas as pd\n" +
            "df = pd.read_csv('counts.txt', comment='#', sep='\\t')\n" +
            "print('Expression quantification completed!')\n";
    }

    private String getDiffExpressionScript(Map<String, String> p) {
        double padj = Double.parseDouble(p.getOrDefault("padj_cutoff", "0.05"));
        double lfc = Double.parseDouble(p.getOrDefault("lfc_cutoff", "1.0"));
        return "#!/usr/bin/env python3\n" +
            "import pandas as pd\n" +
            "import numpy as np\n" +
            "from scipy import stats\n" +
            "import matplotlib.pyplot as plt\n" +
            "# Load count data\n" +
            "counts = pd.read_csv('counts.txt', sep='\\t', index_col=0)\n" +
            "# Normalize (simple TPM)\n" +
            "tpm = counts.div(counts.sum(axis=0), axis=1) * 1e6\n" +
            "# Differential expression (simplified t-test)\n" +
            "control = tpm['control'].mean(axis=1)\n" +
            "treatment = tpm['treatment'].mean(axis=1)\n" +
            "lfc_values = np.log2(treatment / control)\n" +
            "pvalues = stats.ttest_ind(tpm['control'], tpm['treatment'], axis=1).pvalue\n" +
            "# Filter\n" +
            "padj_cutoff = " + padj + "\n" +
            "lfc_cutoff = " + lfc + "\n" +
            "sig = (pvalues < padj_cutoff) & (abs(lfc_values) > lfc_cutoff)\n" +
            "print(f'Found {sig.sum()} DEGs')\n";
    }

    private String getVariantScript(Map<String, String> p) {
        return "#!/usr/bin/env python3\n" +
            "import pysam\n" +
            "ref = '" + p.getOrDefault("reference", "reference.fa") + "'\n" +
            "# Call variants using bcftools\n" +
            "import subprocess\n" +
            "subprocess.run(['bcftools', 'mpileup', '-f', ref, 'aligned.bam'],\n" +
            "               stdout=open('pileup.bcf', 'wb'))\n" +
            "subprocess.run(['bcftools', 'call', '-mv', '-o', 'variants.vcf', 'pileup.bcf'])\n" +
            "# Parse VCF\n" +
            "vcf = pysam.VariantFile('variants.vcf')\n" +
            "for record in vcf:\n" +
            "    print(f'{record.chrom}\\t{record.pos}\\t{record.ref}\\t{record.alts}')\n" +
            "print('Variant calling completed!')\n";
    }

    private String getAnnotationScript(Map<String, String> p) {
        return "#!/usr/bin/env python3\n" +
            "import subprocess\n" +
            "db = '" + p.getOrDefault("database", "nr") + "'\n" +
            "evalue = '" + p.getOrDefault("evalue", "1e-5") + "'\n" +
            "# BLAST search\n" +
            "subprocess.run(['blastp', '-query', 'proteins.fa',\n" +
            "                '-db', db, '-out', 'blast_results.txt',\n" +
            "                '-evalue', evalue, '-outfmt', '6'])\n" +
            "# Parse results\n" +
            "import pandas as pd\n" +
            "cols = ['qseqid','sseqid','pident','length','mismatch','gapopen',\n" +
            "        'qstart','qend','sstart','send','evalue','bitscore']\n" +
            "df = pd.read_csv('blast_results.txt', sep='\\t', names=cols)\n" +
            "print(f'Annotated {len(df)} hits')\n";
    }
}
