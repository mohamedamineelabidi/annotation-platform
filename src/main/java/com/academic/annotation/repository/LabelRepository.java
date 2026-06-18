package com.academic.annotation.repository;

import com.academic.annotation.model.Label;
import com.academic.annotation.model.TaskType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LabelRepository extends JpaRepository<Label, Long> {
    Optional<Label> findByNameAndTaskType(String name, TaskType taskType);

    List<Label> findByTaskTypeOrderByName(TaskType taskType);

    List<Label> findAllByOrderByTaskTypeAscNameAsc();
}
