package com.bioinfo.platform.repository;

import com.bioinfo.platform.entity.TaskResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskResultRepository extends JpaRepository<TaskResult, Long> {
    List<TaskResult> findByTaskId(Long taskId);

    List<TaskResult> findByTaskIdOrderByCreatedAtDesc(Long taskId);

    Optional<TaskResult> findByIdAndTaskId(Long id, Long taskId);
}
