package com.academic.annotation.repository;

import com.academic.annotation.model.Dataset;
import com.academic.annotation.model.Task;
import com.academic.annotation.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByDataset(Dataset dataset);

    List<Task> findByDatasetAndActiveTrue(Dataset dataset);

    List<Task> findByAnnotatorAndActiveTrueOrderByIdDesc(User annotator);

    Optional<Task> findByDatasetAndAnnotatorAndActiveTrue(Dataset dataset, User annotator);
}
