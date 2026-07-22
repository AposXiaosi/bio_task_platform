package com.example.biotaskplatform.controller;

import com.example.biotaskplatform.entity.AnalysisTask;
import com.example.biotaskplatform.service.AnalysisTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/task")
public class AnalysisTaskController {

    @Autowired
    private AnalysisTaskService analysisTaskService;

    /**
     * 查询全部任务
     */
    @GetMapping("/list")
    public List<AnalysisTask> list() {
        return analysisTaskService.list();
    }

    /**
     * 新增任务
     */
    @PostMapping
    public boolean add(@RequestBody AnalysisTask analysisTask) {
        return analysisTaskService.save(analysisTask);
    }
}