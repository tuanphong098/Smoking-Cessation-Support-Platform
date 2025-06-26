package edu.fpt.smokingcessionnew.service;

import edu.fpt.smokingcessionnew.entity.QuitPlan;
import edu.fpt.smokingcessionnew.entity.SmokingHistory;
import edu.fpt.smokingcessionnew.entity.User;
import edu.fpt.smokingcessionnew.repository.QuitPlanRepository;
import edu.fpt.smokingcessionnew.repository.SmokingHistoryRepository;
import edu.fpt.smokingcessionnew.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class QuitPlanService {

    @Autowired
    private QuitPlanRepository quitPlanRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SmokingHistoryRepository smokingHistoryRepository;

    public List<QuitPlan> getUserQuitPlans(Integer userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            return List.of();
        }
        return quitPlanRepository.findByUser(user.get());
    }

    public ResponseEntity<?> createQuitPlan(QuitPlan quitPlan, Integer userId, Integer smokingHistoryId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Người dùng không tồn tại");
        }

        Optional<SmokingHistory> smokingHistoryOptional = smokingHistoryRepository.findById(smokingHistoryId);
        if (smokingHistoryOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Lịch sử hút thuốc không tồn tại");
        }

        User user = userOptional.get();
        SmokingHistory smokingHistory = smokingHistoryOptional.get();

        quitPlan.setUser(user);
        quitPlan.setSmokingHistory(smokingHistory);
        quitPlan.setCreatedDate(LocalDateTime.now()); // Changed from Instant.now() to LocalDateTime.now()
        quitPlan.setStatus(1); // 1 = Active

        QuitPlan savedPlan = quitPlanRepository.save(quitPlan);
        return ResponseEntity.ok(savedPlan);
    }

    public ResponseEntity<?> updateQuitPlan(QuitPlan quitPlan, Integer planId) {
        Optional<QuitPlan> existingPlanOptional = quitPlanRepository.findById(planId);
        if (existingPlanOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Kế hoạch cai thuốc không tồn tại");
        }

        QuitPlan existingPlan = existingPlanOptional.get();

        // Cập nhật các trường có thể chỉnh sửa
        existingPlan.setTargetQuitDate(quitPlan.getTargetQuitDate());
        existingPlan.setQuitMethod(quitPlan.getQuitMethod());
        existingPlan.setDailyReductionTarget(quitPlan.getDailyReductionTarget());
        existingPlan.setMilestoneGoals(quitPlan.getMilestoneGoals());
        existingPlan.setPersonalizedStrategies(quitPlan.getPersonalizedStrategies());
        existingPlan.setStatus(quitPlan.getStatus());

        QuitPlan updatedPlan = quitPlanRepository.save(existingPlan);
        return ResponseEntity.ok(updatedPlan);
    }

    public ResponseEntity<?> getQuitPlan(Integer planId) {
        Optional<QuitPlan> planOptional = quitPlanRepository.findById(planId);
        if (planOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Kế hoạch cai thuốc không tồn tại");
        }
        return ResponseEntity.ok(planOptional.get());
    }

    public ResponseEntity<?> deleteQuitPlan(Integer planId) {
        Optional<QuitPlan> planOptional = quitPlanRepository.findById(planId);
        if (planOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Kế hoạch cai thuốc không tồn tại");
        }

        // Soft delete - đặt status = 0 (không hoạt động)
        QuitPlan plan = planOptional.get();
        plan.setStatus(0);
        quitPlanRepository.save(plan);

        return ResponseEntity.ok("Xóa kế hoạch cai thuốc thành công");
    }

    public List<QuitPlan> getActiveQuitPlans(Integer userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            return List.of();
        }
        return quitPlanRepository.findByUserAndStatus(user.get(), 1); // 1 = Active
    }

    public ResponseEntity<?> getCurrentQuitPlan(Integer userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Người dùng không tồn tại");
        }

        Optional<QuitPlan> currentPlan = quitPlanRepository.findTopByUserOrderByCreatedDateDesc(userOptional.get());
        if (currentPlan.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Người dùng chưa có kế hoạch cai thuốc");
        }

        return ResponseEntity.ok(currentPlan.get());
    }

    public List<QuitPlan> getQuitPlansByDateRange(LocalDate startDate, LocalDate endDate) {
        return quitPlanRepository.findByTargetQuitDateBetween(startDate, endDate);
    }
}
