package com.bioinfo.platform.controller;

import com.bioinfo.platform.entity.AnalysisType;
import com.bioinfo.platform.repository.AnalysisTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analysis-types")
@RequiredArgsConstructor
public class AnalysisTypeController {

    private final AnalysisTypeRepository analysisTypeRepository;

    @GetMapping
    public ResponseEntity<List<AnalysisType>> getAll() {
        List<AnalysisType> types = analysisTypeRepository.findAll();
        return ResponseEntity.ok(types);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnalysisType> getById(@PathVariable Long id) {
        AnalysisType type = analysisTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AnalysisType not found with id: " + id));
        return ResponseEntity.ok(type);
    }
}
