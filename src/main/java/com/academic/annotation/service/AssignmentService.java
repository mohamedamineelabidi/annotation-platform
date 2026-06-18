package com.academic.annotation.service;

import com.academic.annotation.dto.AnnotatorProgress;
import com.academic.annotation.model.Assignment;
import com.academic.annotation.model.DatasetItem;
import com.academic.annotation.model.Role;
import com.academic.annotation.model.User;
import com.academic.annotation.repository.AssignmentRepository;
import com.academic.annotation.repository.DatasetItemRepository;
import com.academic.annotation.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final DatasetItemRepository datasetItemRepository;
    private final UserRepository userRepository;

    public AssignmentService(AssignmentRepository assignmentRepository,
                             DatasetItemRepository datasetItemRepository,
                             UserRepository userRepository) {
        this.assignmentRepository = assignmentRepository;
        this.datasetItemRepository = datasetItemRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public int assignAllItemsToAnnotators(int minimumAnnotators) {
        List<User> annotators = userRepository.findByRoleOrderByUsername(Role.ANNOTATOR).stream()
                .filter(User::isEnabled)
                .toList();
        if (annotators.isEmpty()) {
            return 0;
        }
        int created = 0;
        int required = Math.min(minimumAnnotators, annotators.size());
        for (DatasetItem item : datasetItemRepository.findAll()) {
            long existingCount = assignmentRepository.countByDatasetItem(item);
            for (User annotator : annotators) {
                if (existingCount >= required) {
                    break;
                }
                if (assignmentRepository.findByDatasetItemAndAnnotator(item, annotator).isEmpty()) {
                    Assignment assignment = new Assignment();
                    assignment.setDatasetItem(item);
                    assignment.setAnnotator(annotator);
                    assignmentRepository.save(assignment);
                    existingCount++;
                    created++;
                }
            }
        }
        return created;
    }

    public List<Assignment> findAssignmentsFor(User annotator) {
        return assignmentRepository.findByAnnotatorOrderById(annotator);
    }

    public List<Assignment> findPendingAssignments(User annotator) {
        return assignmentRepository.findByAnnotatorAndCompletedOrderById(annotator, false);
    }

    public List<AnnotatorProgress> progressByUser() {
        List<AnnotatorProgress> progress = new ArrayList<>();
        for (User annotator : userRepository.findByRoleOrderByUsername(Role.ANNOTATOR)) {
            long assigned = assignmentRepository.countByAnnotator(annotator);
            long completed = assignmentRepository.countByAnnotatorAndCompleted(annotator, true);
            double percent = assigned == 0 ? 0.0 : (completed * 100.0 / assigned);
            progress.add(new AnnotatorProgress(annotator.getUsername(), assigned, completed, percent));
        }
        return progress;
    }

    public long totalAssignments() {
        return assignmentRepository.count();
    }

    public long completedAssignments() {
        return assignmentRepository.countByCompleted(true);
    }
}
