package com.academic.annotation.service;

import com.academic.annotation.dto.GeneratedAccount;
import com.academic.annotation.model.Role;
import com.academic.annotation.model.User;
import com.academic.annotation.repository.AnnotationRepository;
import com.academic.annotation.repository.AssignmentRepository;
import com.academic.annotation.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
public class UserService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

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

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Annotator not found"));
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
        return saveAnnotator(id, username, null, null, rawPassword, enabled);
    }

    @Transactional
    public User saveAnnotator(Long id, String username, String firstName, String lastName, String rawPassword, boolean enabled) {
        if (id == null) {
            User user = createUser(username, rawPassword, Role.ANNOTATOR, enabled);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            return userRepository.save(user);
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Annotator not found"));
        user.setUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        if (rawPassword != null && !rawPassword.isBlank()) {
            user.setPassword(passwordEncoder.encode(rawPassword));
        }
        user.setEnabled(enabled);
        user.setRole(Role.ANNOTATOR);
        return userRepository.save(user);
    }

    /**
     * Creates a new annotator with an automatically generated, BCrypt-encoded password.
     * The clear-text password is returned once so the admin can hand it over.
     */
    @Transactional
    public GeneratedAccount createAnnotatorWithGeneratedPassword(String firstName, String lastName, String login) {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("Login is required");
        }
        if (userRepository.existsByUsername(login)) {
            throw new IllegalArgumentException("Login already exists: " + login);
        }
        String rawPassword = generatePassword(10);
        User user = new User();
        user.setUsername(login);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(Role.ANNOTATOR);
        user.setEnabled(true);
        userRepository.save(user);
        return new GeneratedAccount(login, rawPassword);
    }

    public String generatePassword(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(PASSWORD_ALPHABET.charAt(RANDOM.nextInt(PASSWORD_ALPHABET.length())));
        }
        return builder.toString();
    }

    /**
     * Logical deletion: the annotator is disabled but kept in the database so that
     * existing annotations are preserved.
     */
    @Transactional
    public void deactivateAnnotator(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Annotator not found"));
        if (user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Admin account cannot be deactivated here");
        }
        user.setEnabled(false);
        userRepository.save(user);
    }

    @Transactional
    public void reactivateAnnotator(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Annotator not found"));
        user.setEnabled(true);
        userRepository.save(user);
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
