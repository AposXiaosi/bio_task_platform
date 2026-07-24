package com.bioinfo.platform.service;

import com.bioinfo.platform.entity.TaskFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BioinformaticsAnalysisEngineTest {
    @TempDir Path temp;
    private final BioinformaticsAnalysisEngine engine = new BioinformaticsAnalysisEngine(new ObjectMapper());

    @Test
    void runsSequenceWorkflows() throws Exception {
        TaskFile fasta = input("reads.fa", ">reference\nACGTACGT\n>read1\nACGT\n>read2\nTCGT\n");
        assertOutput("sequence_alignment", fasta, "alignment.tsv");
        assertOutput("sequence_assembly", fasta, "contigs.fasta");
        assertOutput("variant_calling", fasta, "variants.vcf");
        assertOutput("functional_annotation", fasta, "functional_annotation.tsv");
    }

    @Test
    void runsExpressionWorkflows() throws Exception {
        TaskFile matrix = input("counts.tsv", "gene\tcontrol_1\tcontrol_2\ttreatment_1\ttreatment_2\nA\t10\t12\t100\t110\nB\t30\t28\t31\t29\n");
        assertOutput("gene_expression", matrix, "normalized_expression.tsv");
        assertOutput("diff_expression", matrix, "differential_expression.tsv");
    }

    private void assertOutput(String type, TaskFile input, String expectedName) throws Exception {
        Path output = temp.resolve(type);
        List<BioinformaticsAnalysisEngine.OutputFile> files = engine.run(type, List.of(input), Map.of(), output, ignored -> {});
        assertThat(files).extracting(BioinformaticsAnalysisEngine.OutputFile::fileName).contains(expectedName, "summary.json");
        assertThat(files).allSatisfy(file -> assertThat(file.path()).exists().isNotEmptyFile());
    }

    private TaskFile input(String name, String content) throws Exception {
        Path path = temp.resolve(name);
        Files.writeString(path, content, StandardCharsets.UTF_8);
        TaskFile file = new TaskFile();
        file.setOriginalName(name);
        file.setFilePath(path.toString());
        file.setFileSize(Files.size(path));
        return file;
    }
}
