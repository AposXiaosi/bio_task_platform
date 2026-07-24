package com.bioinfo.platform.repository;

import com.bioinfo.platform.entity.TaskFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskFileRepository extends JpaRepository<TaskFile, Long> {
    List<TaskFile> findByTaskId(Long taskId);

    List<TaskFile> findByIdInAndTaskIsNull(List<Long> ids);
}
