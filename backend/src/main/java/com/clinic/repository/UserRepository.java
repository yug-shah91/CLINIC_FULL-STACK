package com.clinic.repository;

import com.clinic.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Used in login and JWT filter to find user by email
    Optional<User> findByEmail(String email);

    // Used in register to prevent duplicate registrations
    boolean existsByEmail(String email);

    boolean existsByRole(com.clinic.entity.enums.Role role);
}
