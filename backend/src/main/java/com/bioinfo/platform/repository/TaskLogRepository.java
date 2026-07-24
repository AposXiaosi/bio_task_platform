package com.bioinfo.platform.repository;

import com.bioinfo.platform.entity.TaskLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskLogRepository extends JpaRepository<TaskLog, Long> {
    List<TaskLog> findByTaskIdOrderByCreatedAtAscIdAsc(Long taskId);

    List<TaskLog> findByTaskIdAndLevel(Long taskId, String level);
}
