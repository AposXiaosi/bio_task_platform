package com.bioinfo.platform.repository;

import com.bioinfo.platform.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    @EntityGraph(attributePaths = {"analysisType"})
    java.util.Optional<Task> findById(Long id);
    List<Task> findByStatus(String status);

    List<Task> findByAnalysisTypeId(Long analysisTypeId);

    List<Task> findByNameContaining(String name);

    Page<Task> findByStatus(String status, Pageable pageable);

    Page<Task> findByAnalysisTypeId(Long analysisTypeId, Pageable pageable);

    Page<Task> findByStatusAndAnalysisTypeId(String status, Long analysisTypeId, Pageable pageable);

    Page<Task> findByNameContaining(String name, Pageable pageable);
}
