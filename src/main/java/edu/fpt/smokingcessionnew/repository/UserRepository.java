package edu.fpt.smokingcessionnew.repository;

import edu.fpt.smokingcessionnew.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    Optional<User> findByVerificationToken(String token); // Nếu có trường verification_token
    boolean existsByEmail(String email);

    // Method for finding coaches - role = 3 for COACH
    @Query("SELECT u FROM User u WHERE u.role = :role")
    List<User> findByRole(@Param("role") Integer role);

    // Additional methods for coaching system - using status instead of isActive
    // Assuming status = 1 for active users
    @Query("SELECT u FROM User u WHERE u.role = 3 AND u.status = 1")
    List<User> findActiveCoaches();

    @Query("SELECT u FROM User u WHERE u.role = 3 AND u.status = 1 AND u.id = :coachId")
    Optional<User> findActiveCoachById(@Param("coachId") Integer coachId);

    // Helper method to find users by role value
    List<User> findByRoleAndStatus(Integer role, Integer status);
}