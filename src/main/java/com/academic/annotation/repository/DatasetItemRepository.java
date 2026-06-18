package com.academic.annotation.repository;

import com.academic.annotation.model.DatasetItem;
import com.academic.annotation.model.TaskType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DatasetItemRepository extends JpaRepository<DatasetItem, Long> {
    Optional<DatasetItem> findByExternalId(String externalId);

    List<DatasetItem> findByTaskTypeOrderById(TaskType taskType);
}
