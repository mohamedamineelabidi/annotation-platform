package com.academic.annotation.repository;

import com.academic.annotation.model.Annotation;
import com.academic.annotation.model.DatasetItem;
import com.academic.annotation.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnnotationRepository extends JpaRepository<Annotation, Long> {
    Optional<Annotation> findByDatasetItemAndAnnotator(DatasetItem datasetItem, User annotator);

    List<Annotation> findByAnnotator(User annotator);

    List<Annotation> findByDatasetItem(DatasetItem datasetItem);

    long countByAnnotator(User annotator);

    void deleteByAnnotator(User annotator);
}
