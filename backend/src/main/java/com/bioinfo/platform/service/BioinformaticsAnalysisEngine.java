package com.bioinfo.platform.service;

import com.bioinfo.platform.entity.TaskFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Dependency-free, reproducible analysis implementations used by the demo platform.
 * They intentionally operate on small/medium educational datasets and produce tabular
 * files that can be inspected without installing command-line bioinformatics tools.
 */
@Component
@RequiredArgsConstructor
public class BioinformaticsAnalysisEngine {
    private final ObjectMapper objectMapper;

    public List<OutputFile> run(String type, List<TaskFile> files, Map<String, String> params,
                                Path outputDir, Consumer<String> log) throws Exception {
        Files.createDirectories(outputDir);
        List<Path> input = files.stream().map(f -> Path.of(f.getFilePath())).collect(Collectors.toList());
        if (input.isEmpty()) throw new IllegalArgumentException("未找到可用输入文件");
        return switch (type) {
            case "sequence_alignment" -> alignment(input, params, outputDir, log);
            case "sequence_assembly" -> assembly(input, params, outputDir, log);
            case "gene_expression" -> expression(input, params, outputDir, log);
            case "diff_expression" -> differential(input, params, outputDir, log);
            case "variant_calling" -> variants(input, params, outputDir, log);
            case "functional_annotation" -> annotation(input, params, outputDir, log);
            default -> throw new IllegalArgumentException("不支持的分析类型: " + type);
        };
    }

    private List<OutputFile> alignment(List<Path> input, Map<String, String> p, Path out, Consumer<String> log) throws IOException {
        List<Record> all = readSequences(input, log);
        if (all.isEmpty()) throw new IllegalArgumentException("输入文件中没有识别到 FASTA/FASTQ 序列");
        Record reference = all.get(0);
        List<Record> reads = all.size() > 1 ? all.subList(1, Math.min(all.size(), 5001)) : List.of(reference);
        log.accept("参考序列 " + reference.id() + "，待比对 reads: " + reads.size());
        Path result = out.resolve("alignment.tsv");
        try (BufferedWriter w = writer(result)) {
            w.write("read_id\tposition\tread_length\tmatches\tidentity\tstrand\n");
            for (Record read : reads) {
                Match best = bestMatch(reference.sequence(), read.sequence());
                w.write(String.format(Locale.ROOT, "%s\t%d\t%d\t%d\t%.4f\t%s%n",
                        read.id(), best.position() + 1, read.sequence().length(), best.matches(),
                        best.identity(), best.reverse() ? "-" : "+"));
            }
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("analysis", "sequence_alignment"); summary.put("reference", reference.id());
        summary.put("reads", reads.size()); summary.put("referenceLength", reference.sequence().length());
        return finish(out, List.of(new OutputSpec(result, "每条 read 的最佳无缺口比对位置、匹配数和 identity")), summary, log);
    }

    private List<OutputFile> assembly(List<Path> input, Map<String, String> p, Path out, Consumer<String> log) throws IOException {
        List<Record> reads = readSequences(input, log).stream().limit(500).collect(Collectors.toList());
        if (reads.isEmpty()) throw new IllegalArgumentException("输入文件中没有可组装序列");
        int minOverlap = Math.max(3, integer(p, "min_overlap", integer(p, "readLength", 20) / 3));
        List<String> contigs = reads.stream().map(Record::sequence).distinct().collect(Collectors.toCollection(ArrayList::new));
        log.accept("开始贪心 overlap 组装，reads=" + contigs.size() + ", 最小 overlap=" + minOverlap);
        boolean merged = true;
        while (merged && contigs.size() > 1) {
            merged = false; int bi = -1, bj = -1, best = minOverlap - 1;
            for (int i = 0; i < contigs.size(); i++) for (int j = 0; j < contigs.size(); j++) if (i != j) {
                int overlap = overlap(contigs.get(i), contigs.get(j), minOverlap);
                if (overlap > best) { best = overlap; bi = i; bj = j; }
            }
            if (bi >= 0) {
                String a = contigs.get(bi), b = contigs.get(bj);
                contigs.set(bi, a + b.substring(best)); contigs.remove(bj); merged = true;
            }
        }
        Path fasta = out.resolve("contigs.fasta");
        try (BufferedWriter w = writer(fasta)) { for (int i = 0; i < contigs.size(); i++) w.write(">contig_" + (i + 1) + "\n" + wrap(contigs.get(i)) + "\n"); }
        Path metrics = out.resolve("assembly_metrics.tsv");
        int total = contigs.stream().mapToInt(String::length).sum();
        try (BufferedWriter w = writer(metrics)) { w.write("contigs\ttotal_length\tlongest_contig\tN50\n"); w.write(contigs.size() + "\t" + total + "\t" + contigs.stream().mapToInt(String::length).max().orElse(0) + "\t" + n50(contigs) + "\n"); }
        Map<String, Object> summary = new LinkedHashMap<>(); summary.put("analysis", "sequence_assembly"); summary.put("inputReads", reads.size()); summary.put("contigs", contigs.size()); summary.put("totalLength", total);
        return finish(out, List.of(new OutputSpec(fasta, "贪心 overlap 组装得到的 contigs"), new OutputSpec(metrics, "组装质量指标")), summary, log);
    }

    private List<OutputFile> expression(List<Path> input, Map<String, String> p, Path out, Consumer<String> log) throws IOException {
        Table t = readTable(input.get(0));
        if (t.rows().isEmpty() || t.headers().size() < 2) throw new IllegalArgumentException("表达矩阵至少需要 gene 列和一个样本列");
        double[] totals = new double[t.headers().size() - 1];
        for (List<String> row : t.rows()) for (int i = 1; i < t.headers().size(); i++) totals[i - 1] += number(cell(row, i));
        Path result = out.resolve("normalized_expression.tsv");
        String method = p.getOrDefault("normalizationMethod", p.getOrDefault("normalization", "CPM")).toUpperCase(Locale.ROOT);
        try (BufferedWriter w = writer(result)) {
            w.write(String.join("\t", t.headers())); w.write("\n");
            for (List<String> row : t.rows()) { w.write(cell(row, 0)); for (int i = 1; i < t.headers().size(); i++) { double value = number(cell(row, i)); double normalized = totals[i - 1] == 0 ? 0 : value / totals[i - 1] * 1_000_000; w.write(String.format(Locale.ROOT, "\t%.6f", normalized)); } w.write("\n"); }
        }
        log.accept("已完成 " + method + " 标准化，基因数=" + t.rows().size() + "，样本数=" + (t.headers().size() - 1));
        Map<String, Object> summary = new LinkedHashMap<>(); summary.put("analysis", "gene_expression"); summary.put("normalization", method); summary.put("genes", t.rows().size()); summary.put("samples", t.headers().size() - 1);
        return finish(out, List.of(new OutputSpec(result, "按每百万 reads 标准化的表达矩阵")), summary, log);
    }

    private List<OutputFile> differential(List<Path> input, Map<String, String> p, Path out, Consumer<String> log) throws IOException {
        Table t = readTable(input.get(0)); if (t.headers().size() < 3) throw new IllegalArgumentException("差异表达至少需要两个样本列");
        String control = p.getOrDefault("controlGroup", p.getOrDefault("contrast", "control")).toLowerCase(Locale.ROOT);
        String treatment = p.getOrDefault("treatmentGroup", "treatment").toLowerCase(Locale.ROOT);
        List<Integer> c = new ArrayList<>(), tr = new ArrayList<>();
        for (int i = 1; i < t.headers().size(); i++) { String h = t.headers().get(i).toLowerCase(Locale.ROOT); if (h.contains(treatment)) tr.add(i); else if (h.contains(control)) c.add(i); }
        if (c.isEmpty() || tr.isEmpty()) { for (int i = 1; i < t.headers().size(); i++) (i % 2 == 1 ? c : tr).add(i); log.accept("未从列名识别分组，已按奇偶列拆分 control/treatment"); }
        double pCut = decimal(p, "pValueThreshold", decimal(p, "padj_cutoff", .05)); double fcCut = decimal(p, "foldChange", Math.pow(2, decimal(p, "lfc_cutoff", 1)));
        List<DiffRow> rows = new ArrayList<>();
        for (List<String> row : t.rows()) { double[] a = values(row, c), b = values(row, tr); double meanA = mean(a), meanB = mean(b); double lfc = Math.log((meanB + 1) / (meanA + 1)) / Math.log(2); double pval = normalTwoSidedP(Math.abs(lfc) * Math.sqrt(Math.max(1, a.length + b.length))); rows.add(new DiffRow(cell(row, 0), meanA, meanB, lfc, pval)); }
        rows.sort(Comparator.comparingDouble(DiffRow::pvalue));
        for (int i = 0; i < rows.size(); i++) rows.get(i).padj = Math.min(1, rows.get(i).pvalue() * rows.size() / (i + 1));
        Path result = out.resolve("differential_expression.tsv");
        try (BufferedWriter w = writer(result)) { w.write("gene\tcontrol_mean\ttreatment_mean\tlog2_fold_change\tp_value\tadjusted_p_value\tsignificant\n"); for (DiffRow r : rows) w.write(String.format(Locale.ROOT, "%s\t%.4f\t%.4f\t%.4f\t%.6g\t%.6g\t%s%n", r.gene(), r.control(), r.treatment(), r.log2fc(), r.pvalue(), r.padj, r.padj <= pCut && Math.abs(r.log2fc()) >= Math.log(fcCut) / Math.log(2))); }
        long significant = rows.stream().filter(r -> r.padj <= pCut && Math.abs(r.log2fc()) >= Math.log(fcCut) / Math.log(2)).count(); log.accept("差异表达完成，显著基因=" + significant);
        Map<String, Object> summary = new LinkedHashMap<>(); summary.put("analysis", "diff_expression"); summary.put("genes", rows.size()); summary.put("significantGenes", significant); summary.put("controlSamples", c.size()); summary.put("treatmentSamples", tr.size());
        return finish(out, List.of(new OutputSpec(result, "含 log2FC、p 值和 BH 校正结果的差异表达表")), summary, log);
    }

    private List<OutputFile> variants(List<Path> input, Map<String, String> p, Path out, Consumer<String> log) throws IOException {
        List<Record> seqs = readSequences(input, log); if (seqs.size() < 2) throw new IllegalArgumentException("变异检测需要参考序列和至少一个样本序列");
        String ref = seqs.get(0).sequence(), sample = seqs.get(1).sequence(); Path result = out.resolve("variants.vcf"); int count = 0;
        try (BufferedWriter w = writer(result)) { w.write("##fileformat=VCFv4.3\n##source=BioInfoPlatformBuiltinCaller\n#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\n"); for (int i = 0; i < Math.min(ref.length(), sample.length()); i++) if (Character.toUpperCase(ref.charAt(i)) != Character.toUpperCase(sample.charAt(i))) { count++; w.write("reference\t" + (i + 1) + "\t.\t" + ref.charAt(i) + "\t" + sample.charAt(i) + "\t60\tPASS\tTYPE=SNP\n"); } }
        log.accept("变异检测完成，发现 SNP=" + count); Map<String, Object> summary = new LinkedHashMap<>(); summary.put("analysis", "variant_calling"); summary.put("variants", count); summary.put("referenceLength", ref.length());
        return finish(out, List.of(new OutputSpec(result, "VCF 格式的单碱基变异")), summary, log);
    }

    private List<OutputFile> annotation(List<Path> input, Map<String, String> p, Path out, Consumer<String> log) throws IOException {
        List<Record> seqs = readSequences(input, log); if (seqs.isEmpty()) throw new IllegalArgumentException("输入文件中没有序列"); Path result = out.resolve("functional_annotation.tsv");
        try (BufferedWriter w = writer(result)) { w.write("sequence_id\tsequence_type\tlength\tgc_percent\tpredicted_function\n"); for (Record r : seqs) { String seq = r.sequence().toUpperCase(Locale.ROOT); int gc = (int) seq.chars().filter(c -> c == 'G' || c == 'C').count(); String type = seq.matches("[ACGTN]+") ? "nucleotide" : "protein"; String fn = type.equals("protein") ? proteinFunction(seq) : nucleotideFunction(seq); w.write(String.format(Locale.ROOT, "%s\t%s\t%d\t%.2f\t%s%n", r.id(), type, seq.length(), seq.isEmpty() ? 0 : gc * 100.0 / seq.length(), fn)); } }
        log.accept("功能注释完成，序列数=" + seqs.size()); Map<String, Object> summary = new LinkedHashMap<>(); summary.put("analysis", "functional_annotation"); summary.put("sequences", seqs.size());
        return finish(out, List.of(new OutputSpec(result, "基于序列长度、GC 和 motif 的可解释注释结果")), summary, log);
    }

    private List<OutputFile> finish(Path out, List<OutputSpec> specs, Map<String, Object> summary, Consumer<String> log) throws IOException {
        Path json = out.resolve("summary.json"); objectMapper.writerWithDefaultPrettyPrinter().writeValue(json.toFile(), summary); List<OutputFile> result = new ArrayList<>();
        for (OutputSpec s : specs) { result.add(new OutputFile(s.path(), s.path().getFileName().toString(), s.description())); log.accept("已生成结果文件: " + s.path().getFileName()); }
        result.add(new OutputFile(json, "summary.json", "分析摘要（JSON）")); log.accept("分析摘要已写出"); return result;
    }

    private List<Record> readSequences(List<Path> paths, Consumer<String> log) throws IOException { List<Record> result = new ArrayList<>(); for (Path path : paths) { if (!Files.exists(path)) throw new IOException("输入文件不存在: " + path); try (BufferedReader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { String line, id = null; StringBuilder seq = new StringBuilder(); boolean fastq = path.getFileName().toString().toLowerCase(Locale.ROOT).matches(".*\\.(fq|fastq)$"); int lineNo = 0, qualitySkip = 0; while ((line = r.readLine()) != null && result.size() < 10000) { lineNo++; line = line.trim(); if (line.isEmpty()) continue; if (fastq) { if (lineNo % 4 == 1) { id = line.startsWith("@") ? line.substring(1).split("\\s+", 2)[0] : "read_" + result.size(); } else if (lineNo % 4 == 2) { result.add(new Record(id, line.toUpperCase(Locale.ROOT))); } } else if (line.startsWith(">")) { if (id != null && seq.length() > 0) result.add(new Record(id, seq.toString())); id = line.substring(1).split("\\s+", 2)[0]; seq = new StringBuilder(); } else { if (id == null) id = path.getFileName().toString(); seq.append(line.replaceAll("\\s+", "").toUpperCase(Locale.ROOT)); } } if (!fastq && id != null && seq.length() > 0 && result.size() < 10000) result.add(new Record(id, seq.toString())); } } log.accept("解析序列记录 " + result.size() + " 条"); return result; }

    private Table readTable(Path path) throws IOException { try (BufferedReader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { String first = r.readLine(); if (first == null) return new Table(List.of(), List.of()); String delimiter = first.contains("\t") ? "\t" : ","; List<String> headers = split(first, delimiter); List<List<String>> rows = new ArrayList<>(); String line; while ((line = r.readLine()) != null && rows.size() < 100000) { if (!line.isBlank()) rows.add(split(line, delimiter)); } return new Table(headers, rows); } }
    private List<String> split(String line, String delimiter) { return Arrays.stream(line.split(delimiter, -1)).map(String::trim).collect(Collectors.toList()); }
    private BufferedWriter writer(Path p) throws IOException { return Files.newBufferedWriter(p, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING); }
    private String cell(List<String> row, int i) { return i < row.size() ? row.get(i) : "0"; }
    private double number(String v) { try { return Double.parseDouble(v.replace(",", "")); } catch (Exception e) { return 0; } }
    private double[] values(List<String> row, List<Integer> indexes) { double[] a = new double[indexes.size()]; for (int i = 0; i < a.length; i++) a[i] = number(cell(row, indexes.get(i))); return a; }
    private double mean(double[] a) { return Arrays.stream(a).average().orElse(0); }
    private int integer(Map<String, String> p, String k, int d) { try { return Integer.parseInt(p.getOrDefault(k, String.valueOf(d))); } catch (Exception e) { return d; } }
    private double decimal(Map<String, String> p, String k, double d) { try { return Double.parseDouble(p.getOrDefault(k, String.valueOf(d))); } catch (Exception e) { return d; } }
    private int overlap(String a, String b, int min) { for (int n = Math.min(a.length(), b.length()); n >= min; n--) if (a.regionMatches(a.length() - n, b, 0, n)) return n; return -1; }
    private int n50(List<String> c) { List<Integer> l = c.stream().map(String::length).sorted(Comparator.reverseOrder()).collect(Collectors.toList()); int total = l.stream().mapToInt(Integer::intValue).sum(), sum = 0; for (int x : l) { sum += x; if (sum * 2 >= total) return x; } return 0; }
    private String wrap(String s) { StringBuilder b = new StringBuilder(); for (int i = 0; i < s.length(); i += 80) b.append(s, i, Math.min(i + 80, s.length())).append('\n'); return b.toString().trim(); }
    private Match bestMatch(String ref, String read) { Match best = new Match(0, -1, 0, false); for (boolean reverse : new boolean[]{false, true}) { String s = reverse ? reverseComplement(read) : read; int compared = Math.min(ref.length(), s.length()); for (int pos = 0; pos <= Math.max(0, ref.length() - compared); pos++) { int matches = 0; for (int i = 0; i < compared; i++) if (Character.toUpperCase(ref.charAt(pos + i)) == Character.toUpperCase(s.charAt(i))) matches++; if (matches > best.matches()) best = new Match(pos, matches, s.isEmpty() ? 0 : matches * 1.0 / s.length(), reverse); } } return best; }
    private String reverseComplement(String s) { StringBuilder b = new StringBuilder(); for (int i = s.length() - 1; i >= 0; i--) b.append(switch (s.charAt(i)) { case 'A' -> 'T'; case 'T' -> 'A'; case 'C' -> 'G'; case 'G' -> 'C'; default -> 'N'; }); return b.toString(); }
    private String proteinFunction(String s) { if (s.matches(".*HGG.H.*")) return "可能的金属酶活性位点"; if (s.matches(".*G....GKS.*")) return "可能的 P-loop 核苷酸结合蛋白"; return "未发现内置 motif（需 BLAST/InterPro 进一步确认）"; }
    private String nucleotideFunction(String s) { if (s.contains("ATG")) return "含起始密码子，可能为编码序列"; return "核酸序列（未发现起始密码子）"; }
    private double normalTwoSidedP(double z) { double c = 0.5 * (1 + erf(z / Math.sqrt(2))); return Math.max(1e-12, 2 * (1 - c)); }
    private double erf(double x) { double sign = x < 0 ? -1 : 1, a = Math.abs(x), t = 1 / (1 + .3275911 * a); double y = 1 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - .284496736) * t + .254829592) * t * Math.exp(-a * a); return sign * y; }

    private record Record(String id, String sequence) {}
    private record Match(int position, int matches, double identity, boolean reverse) {}
    private record Table(List<String> headers, List<List<String>> rows) {}
    private record OutputSpec(Path path, String description) {}
    public record OutputFile(Path path, String fileName, String description) {}
    private static class DiffRow { private final String gene; private final double control, treatment, log2fc, pvalue; private double padj; DiffRow(String g, double c, double t, double l, double p) { gene=g; control=c; treatment=t; log2fc=l; pvalue=p; } String gene(){return gene;} double control(){return control;} double treatment(){return treatment;} double log2fc(){return log2fc;} double pvalue(){return pvalue;} }
}
