package com.bioinfo.platform.repository;

import com.bioinfo.platform.entity.TaskParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskParameterRepository extends JpaRepository<TaskParameter, Long> {
    List<TaskParameter> findByTaskId(Long taskId);
}
