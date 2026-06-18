package com.academic.annotation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academic.annotation.model.Dataset;

public interface DatasetRepository extends JpaRepository<Dataset, Long> {
    List<Dataset> findAllByOrderByCreatedAtDesc();
}
