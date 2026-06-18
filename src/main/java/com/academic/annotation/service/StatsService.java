package com.academic.annotation.service;

import com.academic.annotation.dto.ClassCount;
import com.academic.annotation.model.Annotation;
import com.academic.annotation.model.DatasetItem;
import com.academic.annotation.model.Label;
import com.academic.annotation.model.Role;
import com.academic.annotation.model.User;
import com.academic.annotation.repository.AnnotationRepository;
import com.academic.annotation.repository.AssignmentRepository;
import com.academic.annotation.repository.DatasetItemRepository;
import com.academic.annotation.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatsService {

    private final UserRepository userRepository;
    private final DatasetItemRepository datasetItemRepository;
    private final AssignmentRepository assignmentRepository;
    private final AnnotationRepository annotationRepository;

    public StatsService(UserRepository userRepository,
                        DatasetItemRepository datasetItemRepository,
                        AssignmentRepository assignmentRepository,
                        AnnotationRepository annotationRepository) {
        this.userRepository = userRepository;
        this.datasetItemRepository = datasetItemRepository;
        this.assignmentRepository = assignmentRepository;
        this.annotationRepository = annotationRepository;
    }

    public Map<String, Object> globalStats() {
        Map<String, Object> stats = new HashMap<>();
        long totalAssignments = assignmentRepository.count();
        long completedAssignments = assignmentRepository.countByCompleted(true);
        stats.put("annotators", userRepository.findByRoleOrderByUsername(Role.ANNOTATOR).size());
        stats.put("items", datasetItemRepository.count());
        stats.put("assignments", totalAssignments);
        stats.put("completedAssignments", completedAssignments);
        stats.put("annotations", annotationRepository.count());
        stats.put("progress", totalAssignments == 0 ? 0.0 : completedAssignments * 100.0 / totalAssignments);
        stats.put("agreementRate", agreementRate());
        stats.put("globalDistribution", globalDistribution());
        return stats;
    }

    public double agreementRate() {
        int denominator = 0;
        int majorityItems = 0;
        for (DatasetItem item : datasetItemRepository.findAll()) {
            List<Annotation> annotations = annotationRepository.findByDatasetItem(item);
            if (annotations.size() < 2) {
                continue;
            }
            denominator++;
            Map<Label, Long> grouped = annotations.stream()
                    .collect(Collectors.groupingBy(Annotation::getLabel, Collectors.counting()));
            long max = grouped.values().stream().mapToLong(Long::longValue).max().orElse(0);
            if (max > annotations.size() / 2.0) {
                majorityItems++;
            }
        }
        return denominator == 0 ? 0.0 : majorityItems * 100.0 / denominator;
    }

    public List<ClassCount> globalDistribution() {
        Map<String, Long> counts = annotationRepository.findAll().stream()
                .collect(Collectors.groupingBy(a -> a.getLabel().getName(), Collectors.counting()));
        return counts.entrySet().stream()
                .map(entry -> new ClassCount(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(ClassCount::labelName))
                .toList();
    }

    public Map<String, Object> personalStats(User user) {
        long assigned = assignmentRepository.countByAnnotator(user);
        long completed = assignmentRepository.countByAnnotatorAndCompleted(user, true);
        Map<String, Object> stats = new HashMap<>();
        stats.put("assigned", assigned);
        stats.put("completed", completed);
        stats.put("progress", assigned == 0 ? 0.0 : completed * 100.0 / assigned);
        stats.put("annotations", annotationRepository.countByAnnotator(user));
        return stats;
    }
}
