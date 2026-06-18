package com.academic.annotation.repository;

import com.academic.annotation.model.Role;
import com.academic.annotation.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findByRoleOrderByUsername(Role role);
}
