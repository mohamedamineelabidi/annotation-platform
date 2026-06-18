package com.academic.annotation.repository;

import com.academic.annotation.model.DatasetItem;
import com.academic.annotation.model.Task;
import com.academic.annotation.model.TaskItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskItemRepository extends JpaRepository<TaskItem, Long> {
    List<TaskItem> findByTaskOrderById(Task task);

    Optional<TaskItem> findByTaskAndDatasetItem(Task task, DatasetItem datasetItem);

    long countByTask(Task task);

    long countByTaskAndCompleted(Task task, boolean completed);

    boolean existsByTaskAndDatasetItem(Task task, DatasetItem datasetItem);

    void deleteByTaskAndCompletedFalse(Task task);
}
