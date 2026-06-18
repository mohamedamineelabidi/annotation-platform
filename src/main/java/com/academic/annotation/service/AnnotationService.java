package com.academic.annotation.service;

import com.academic.annotation.dto.ClassCount;
import com.academic.annotation.model.Annotation;
import com.academic.annotation.model.Assignment;
import com.academic.annotation.model.DatasetItem;
import com.academic.annotation.model.Label;
import com.academic.annotation.model.User;
import com.academic.annotation.repository.AnnotationRepository;
import com.academic.annotation.repository.AssignmentRepository;
import com.academic.annotation.repository.DatasetItemRepository;
import com.academic.annotation.repository.LabelRepository;
import com.academic.annotation.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AnnotationService {

    private final AnnotationRepository annotationRepository;
    private final AssignmentRepository assignmentRepository;
    private final DatasetItemRepository datasetItemRepository;
    private final LabelRepository labelRepository;
    private final UserRepository userRepository;

    public AnnotationService(AnnotationRepository annotationRepository,
                             AssignmentRepository assignmentRepository,
                             DatasetItemRepository datasetItemRepository,
                             LabelRepository labelRepository,
                             UserRepository userRepository) {
        this.annotationRepository = annotationRepository;
        this.assignmentRepository = assignmentRepository;
        this.datasetItemRepository = datasetItemRepository;
        this.labelRepository = labelRepository;
        this.userRepository = userRepository;
    }

    public List<Assignment> assignedTo(String username) {
        User annotator = findUser(username);
        return assignmentRepository.findByAnnotatorOrderById(annotator);
    }

    public Optional<Assignment> firstPending(String username) {
        User annotator = findUser(username);
        return assignmentRepository.findByAnnotatorAndCompletedOrderById(annotator, false).stream().findFirst();
    }

    public Optional<Assignment> assignmentFor(String username, Long itemId) {
        User annotator = findUser(username);
        DatasetItem item = datasetItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Text not found"));
        return assignmentRepository.findByDatasetItemAndAnnotator(item, annotator);
    }

    public Optional<Annotation> existingAnnotation(String username, DatasetItem item) {
        return annotationRepository.findByDatasetItemAndAnnotator(item, findUser(username));
    }

    @Transactional
    public void saveAnnotation(String username, Long itemId, Long labelId, long startedAtMillis) {
        User annotator = findUser(username);
        DatasetItem item = datasetItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Text not found"));
        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new IllegalArgumentException("Label not found"));
        if (label.getTaskType() != item.getTaskType()) {
            throw new IllegalArgumentException("Selected label is not valid for this task type");
        }
        Annotation annotation = annotationRepository.findByDatasetItemAndAnnotator(item, annotator)
                .orElseGet(() -> {
                    Annotation created = new Annotation();
                    created.setDatasetItem(item);
                    created.setAnnotator(annotator);
                    return created;
                });
        annotation.setLabel(label);
        annotation.setAnnotationDate(LocalDateTime.now());
        long seconds = Math.max(1, (System.currentTimeMillis() - startedAtMillis) / 1000);
        annotation.setTimeSpentSeconds(seconds);
        annotationRepository.save(annotation);

        assignmentRepository.findByDatasetItemAndAnnotator(item, annotator).ifPresent(assignment -> {
            assignment.setCompleted(true);
            assignmentRepository.save(assignment);
        });
    }

    public List<ClassCount> classDistribution(User annotator) {
        Map<String, Long> counts = annotationRepository.findByAnnotator(annotator).stream()
                .collect(Collectors.groupingBy(a -> a.getLabel().getName(), Collectors.counting()));
        return counts.entrySet().stream()
                .map(entry -> new ClassCount(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(ClassCount::labelName))
                .toList();
    }

    public double averageTime(User annotator) {
        List<Annotation> annotations = annotationRepository.findByAnnotator(annotator);
        return annotations.stream().mapToLong(Annotation::getTimeSpentSeconds).average().orElse(0.0);
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Unknown user: " + username));
    }
}
