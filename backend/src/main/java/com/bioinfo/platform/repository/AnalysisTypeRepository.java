package com.bioinfo.platform.repository;

import com.bioinfo.platform.entity.AnalysisType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalysisTypeRepository extends JpaRepository<AnalysisType, Long> {
}
