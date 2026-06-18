package com.academic.annotation.repository;

import com.academic.annotation.model.TrainingRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainingRunRepository extends JpaRepository<TrainingRun, Long> {
    List<TrainingRun> findAllByOrderByStartedAtDesc();
}
