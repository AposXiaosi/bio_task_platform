package com.example.biotaskplatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.biotaskplatform.entity.AnalysisTask;
import com.example.biotaskplatform.mapper.AnalysisTaskMapper;
import com.example.biotaskplatform.service.AnalysisTaskService;
import org.springframework.stereotype.Service;

@Service
public class AnalysisTaskServiceImpl
        extends ServiceImpl<AnalysisTaskMapper, AnalysisTask>
        implements AnalysisTaskService {

}