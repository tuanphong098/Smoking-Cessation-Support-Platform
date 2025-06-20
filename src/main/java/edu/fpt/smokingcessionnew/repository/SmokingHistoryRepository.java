package edu.fpt.smokingcessionnew.repository;

import edu.fpt.smokingcessionnew.entity.SmokingHistory;
import edu.fpt.smokingcessionnew.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SmokingHistoryRepository extends JpaRepository<SmokingHistory, Integer> {
    List<SmokingHistory> findByUser(User user);
    Optional<SmokingHistory> findTopByUserOrderByRecordedDateDesc(User user);
}
