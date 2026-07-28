package com.tutorial.user.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByPhoneNumber(String phoneNumber);

    @Query("SELECT u.id FROM User u WHERE u.phoneNumber = :phoneNumber")
    Optional<Long> findIdByPhoneNumber(String phoneNumber);

    boolean existsByUsername(String username);
    boolean existsByPhoneNumber(String phoneNumber);
}
