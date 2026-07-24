package com.bioinfo.platform.config;

import com.bioinfo.platform.entity.AnalysisType;
import com.bioinfo.platform.entity.SysUser;
import com.bioinfo.platform.repository.AnalysisTypeRepository;
import com.bioinfo.platform.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/** Keeps a fresh development database usable without a manual SQL import. */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final AnalysisTypeRepository analysisTypeRepository;
    private final SysUserRepository sysUserRepository;

    @Override
    public void run(String... args) {
        if (analysisTypeRepository.count() == 0) {
            analysisTypeRepository.saveAll(List.of(
                    type("序列比对", "sequence_alignment", "将测序 reads 比对到参考基因组", "{\"tool\":\"bwa\",\"threads\":4}"),
                    type("序列组装", "sequence_assembly", "从测序 reads 拼接重建完整序列", "{\"tool\":\"spades\",\"threads\":4}"),
                    type("基因表达分析", "gene_expression", "定量基因表达水平并生成表达矩阵", "{\"aligner\":\"star\",\"threads\":4}"),
                    type("差异表达分析", "diff_expression", "比较不同条件间的基因表达差异", "{\"tool\":\"deseq2\",\"padj_cutoff\":0.05}"),
                    type("变异检测", "variant_calling", "检测基因组中的 SNP 和 InDel 变异", "{\"tool\":\"gatk\",\"threads\":4}"),
                    type("功能注释", "functional_annotation", "对基因或蛋白质进行功能注释与富集分析", "{\"tool\":\"blast\",\"threads\":4}")
            ));
        }

        if (!sysUserRepository.existsByUsername("admin")) {
            SysUser admin = new SysUser();
            admin.setUsername("admin");
            admin.setPassword("admin123");
            admin.setNickname("平台管理员");
            admin.setEmail("admin@bioinfo.local");
            admin.setStatus(1);
            sysUserRepository.save(admin);
        }
    }

    private AnalysisType type(String name, String code, String description, String params) {
        AnalysisType type = new AnalysisType();
        type.setName(name);
        type.setCode(code);
        type.setDescription(description);
        type.setDefaultParams(params);
        return type;
    }
}
