package edu.fpt.smokingcessionnew.repository;

import edu.fpt.smokingcessionnew.entity.CoachingSession;
import edu.fpt.smokingcessionnew.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoachingSessionRepository extends JpaRepository<CoachingSession, Integer> {

    List<CoachingSession> findByMember(User member);

    List<CoachingSession> findByCoach(User coach);

    @Query("SELECT cs FROM CoachingSession cs WHERE cs.coach.id = :coachId AND cs.status = :status")
    List<CoachingSession> findByCoachIdAndStatus(@Param("coachId") Integer coachId, @Param("status") Integer status);

    @Query("SELECT cs FROM CoachingSession cs WHERE cs.member.id = :memberId AND cs.status = :status")
    List<CoachingSession> findByMemberIdAndStatus(@Param("memberId") Integer memberId, @Param("status") Integer status);

    List<CoachingSession> findBySessionType(Integer sessionType);

    @Query("SELECT cs FROM CoachingSession cs WHERE cs.coach.id = :coachId ORDER BY cs.createdDate DESC")
    List<CoachingSession> findByCoachIdOrderByCreatedDateDesc(@Param("coachId") Integer coachId);

    @Query("SELECT cs FROM CoachingSession cs WHERE cs.member.id = :memberId ORDER BY cs.createdDate DESC")
    List<CoachingSession> findByMemberIdOrderByCreatedDateDesc(@Param("memberId") Integer memberId);
}
