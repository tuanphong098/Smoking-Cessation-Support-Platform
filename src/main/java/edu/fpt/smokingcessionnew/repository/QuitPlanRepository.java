package edu.fpt.smokingcessionnew.repository;

import edu.fpt.smokingcessionnew.entity.QuitPlan;
import edu.fpt.smokingcessionnew.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuitPlanRepository extends JpaRepository<QuitPlan, Integer> {
    List<QuitPlan> findByUser(User user);
    List<QuitPlan> findByUserAndStatus(User user, Integer status);
    List<QuitPlan> findByTargetQuitDateBetween(LocalDate startDate, LocalDate endDate);
    Optional<QuitPlan> findTopByUserOrderByCreatedDateDesc(User user);

    // Methods for coach to view member plans
    List<QuitPlan> findByCoach(User coach);
    List<QuitPlan> findByCoachAndStatus(User coach, Integer status);
    List<QuitPlan> findByCoachOrderByCreatedDateDesc(User coach);
    List<QuitPlan> findByCoachAndStatusOrderByCreatedDateDesc(User coach, Integer status);
}
