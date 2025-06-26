package edu.fpt.smokingcessionnew.repository;

import edu.fpt.smokingcessionnew.entity.MemberSubscription;
import edu.fpt.smokingcessionnew.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberSubscriptionRepository extends JpaRepository<MemberSubscription, Integer> {
    List<MemberSubscription> findByUserAndStatus(User user, Integer status);
    Optional<MemberSubscription> findByTransactionId(String transactionId);
    Optional<MemberSubscription> findTopByUserOrderByEndDateDesc(User user);
}
