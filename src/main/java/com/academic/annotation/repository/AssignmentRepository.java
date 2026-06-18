package com.academic.annotation.repository;

import com.academic.annotation.model.Assignment;
import com.academic.annotation.model.DatasetItem;
import com.academic.annotation.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByAnnotatorOrderById(User annotator);

    List<Assignment> findByAnnotatorAndCompletedOrderById(User annotator, boolean completed);

    Optional<Assignment> findByDatasetItemAndAnnotator(DatasetItem datasetItem, User annotator);

    List<Assignment> findByDatasetItem(DatasetItem datasetItem);

    long countByDatasetItem(DatasetItem datasetItem);

    void deleteByAnnotator(User annotator);

    long countByAnnotator(User annotator);

    long countByAnnotatorAndCompleted(User annotator, boolean completed);

    long countByCompleted(boolean completed);
}
