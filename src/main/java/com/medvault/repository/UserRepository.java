package com.medvault.repository;

import com.medvault.model.User;
import com.medvault.model.enums.Role;
import org. springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);
    Optional<User> findByPasswordResetToken(String token);
    List<User> findByRole(Role role);
}