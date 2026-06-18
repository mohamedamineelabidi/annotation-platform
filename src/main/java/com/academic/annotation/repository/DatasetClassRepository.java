package com.academic.annotation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academic.annotation.model.Dataset;
import com.academic.annotation.model.DatasetClass;

public interface DatasetClassRepository extends JpaRepository<DatasetClass, Long> {
    List<DatasetClass> findByDatasetOrderById(Dataset dataset);
}
