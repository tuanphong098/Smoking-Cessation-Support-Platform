package edu.fpt.smokingcessionnew.repository;

import edu.fpt.smokingcessionnew.entity.DailyProgress;
import edu.fpt.smokingcessionnew.entity.QuitPlan;
import edu.fpt.smokingcessionnew.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyProgressRepository extends JpaRepository<DailyProgress, Integer> {
    List<DailyProgress> findByUser(User user);
    List<DailyProgress> findByUserAndDateBetween(User user, LocalDate startDate, LocalDate endDate);
    List<DailyProgress> findByPlan(QuitPlan plan);
    Optional<DailyProgress> findByUserAndDate(User user, LocalDate date);
}
