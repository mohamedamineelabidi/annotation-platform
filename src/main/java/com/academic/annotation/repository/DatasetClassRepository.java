package com.academic.annotation.repository;

import com.academic.annotation.model.Dataset;
import com.academic.annotation.model.DatasetClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DatasetClassRepository extends JpaRepository<DatasetClass, Long> {
    List<DatasetClass> findByDatasetOrderById(Dataset dataset);
}
