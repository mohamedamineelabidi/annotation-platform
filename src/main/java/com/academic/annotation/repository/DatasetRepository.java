package com.academic.annotation.repository;

import com.academic.annotation.model.Dataset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DatasetRepository extends JpaRepository<Dataset, Long> {
    List<Dataset> findAllByOrderByCreatedAtDesc();
}
