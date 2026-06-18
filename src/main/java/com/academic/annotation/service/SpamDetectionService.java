package com.academic.annotation.service;

import com.academic.annotation.dto.SuspiciousUser;
import com.academic.annotation.model.Annotation;
import com.academic.annotation.model.Role;
import com.academic.annotation.model.User;
import com.academic.annotation.repository.AnnotationRepository;
import com.academic.annotation.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SpamDetectionService {

    private final UserRepository userRepository;
    private final AnnotationRepository annotationRepository;

    public SpamDetectionService(UserRepository userRepository, AnnotationRepository annotationRepository) {
        this.userRepository = userRepository;
        this.annotationRepository = annotationRepository;
    }

    public List<SuspiciousUser> detectSuspiciousUsers() {
        List<SuspiciousUser> suspicious = new ArrayList<>();
        for (User user : userRepository.findByRoleOrderByUsername(Role.ANNOTATOR)) {
            List<Annotation> annotations = annotationRepository.findByAnnotator(user);
            double averageTime = annotations.stream().mapToLong(Annotation::getTimeSpentSeconds).average().orElse(0.0);
            double dominantRate = dominantLabelRate(annotations);
            List<String> reasons = new ArrayList<>();
            if (annotations.size() >= 3 && averageTime < 2.0) {
                reasons.add("Average annotation time below 2 seconds");
            }
            if (annotations.size() >= 5 && dominantRate > 90.0) {
                reasons.add("One label used more than 90% of the time");
            }
            if (!reasons.isEmpty()) {
                suspicious.add(new SuspiciousUser(user.getUsername(), String.join("; ", reasons), averageTime, dominantRate));
            }
        }
        return suspicious;
    }

    private double dominantLabelRate(List<Annotation> annotations) {
        if (annotations.isEmpty()) {
            return 0.0;
        }
        Map<Long, Long> counts = annotations.stream()
                .collect(Collectors.groupingBy(a -> a.getLabel().getId(), Collectors.counting()));
        long max = counts.values().stream().mapToLong(Long::longValue).max().orElse(0);
        return max * 100.0 / annotations.size();
    }
}
