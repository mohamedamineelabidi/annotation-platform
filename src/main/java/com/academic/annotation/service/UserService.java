package com.academic.annotation.service;

import com.academic.annotation.model.Role;
import com.academic.annotation.model.User;
import com.academic.annotation.repository.AnnotationRepository;
import com.academic.annotation.repository.AssignmentRepository;
import com.academic.annotation.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final AnnotationRepository annotationRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       AssignmentRepository assignmentRepository,
                       AnnotationRepository annotationRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.annotationRepository = annotationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> findAnnotators() {
        return userRepository.findByRoleOrderByUsername(Role.ANNOTATOR);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Unknown user: " + username));
    }

    @Transactional
    public User createUser(String username, String rawPassword, Role role, boolean enabled) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setEnabled(enabled);
        return userRepository.save(user);
    }

    @Transactional
    public User saveAnnotator(Long id, String username, String rawPassword, boolean enabled) {
        if (id == null) {
            return createUser(username, rawPassword, Role.ANNOTATOR, enabled);
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Annotator not found"));
        user.setUsername(username);
        if (rawPassword != null && !rawPassword.isBlank()) {
            user.setPassword(passwordEncoder.encode(rawPassword));
        }
        user.setEnabled(enabled);
        user.setRole(Role.ANNOTATOR);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteAnnotator(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Annotator not found"));
        if (user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Admin account cannot be deleted here");
        }
        annotationRepository.deleteByAnnotator(user);
        assignmentRepository.deleteByAnnotator(user);
        userRepository.delete(user);
    }

    @Transactional
    public User ensureUser(String username, String rawPassword, Role role) {
        return userRepository.findByUsername(username).orElseGet(() -> createUser(username, rawPassword, role, true));
    }
}
