package edu.fpt.smokingcessionnew.service;

import edu.fpt.smokingcessionnew.entity.DailyProgress;
import edu.fpt.smokingcessionnew.entity.QuitPlan;
import edu.fpt.smokingcessionnew.entity.User;
import edu.fpt.smokingcessionnew.repository.DailyProgressRepository;
import edu.fpt.smokingcessionnew.repository.QuitPlanRepository;
import edu.fpt.smokingcessionnew.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class DailyProgressService {

    @Autowired
    private DailyProgressRepository dailyProgressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuitPlanRepository quitPlanRepository;

    public List<DailyProgress> getUserProgress(Integer userId) {
        Optional<User> user = userRepository.findById(userId);
        return user.map(value -> dailyProgressRepository.findByUser(value)).orElse(List.of());
    }

    public ResponseEntity<?> recordDailyProgress(DailyProgress dailyProgress, Integer userId, Integer planId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Người dùng không tồn tại");
        }

        Optional<QuitPlan> planOptional = quitPlanRepository.findById(planId);
        if (planOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Kế hoạch cai thuốc không tồn tại");
        }

        User user = userOptional.get();
        QuitPlan plan = planOptional.get();

        // Kiểm tra xem đã có bản ghi cho ngày hôm nay chưa
        Optional<DailyProgress> existingProgress = dailyProgressRepository.findByUserAndDate(user, dailyProgress.getDate());
        if (existingProgress.isPresent()) {
            return ResponseEntity.badRequest().body("Đã có bản ghi tiến độ cho ngày này");
        }

        // Tính toán số tiền tiết kiệm dựa trên số điếu thuốc không hút và giá thuốc lá
        BigDecimal pricePerPack = plan.getSmokingHistory().getPricePerPack();
        int cigarettesPerDay = plan.getSmokingHistory().getCigarettesPerDay();
        int cigarettesSmoked = dailyProgress.getCigarettesSmoked();

        // Giả sử 1 gói = 20 điếu thuốc
        BigDecimal moneySaved = pricePerPack.multiply(BigDecimal.valueOf((cigarettesPerDay - cigarettesSmoked) / 20.0));

        dailyProgress.setUser(user);
        dailyProgress.setPlan(plan);
        dailyProgress.setMoneySaved(moneySaved);
        dailyProgress.setCreatedDate(Instant.now());

        // Tính toán health score dựa trên số điếu hút và mức độ thèm thuốc
        int healthScore = calculateHealthScore(cigarettesSmoked, cigarettesPerDay, dailyProgress.getCravingIntensity());
        dailyProgress.setHealthScore(healthScore);

        DailyProgress savedProgress = dailyProgressRepository.save(dailyProgress);
        return ResponseEntity.ok(savedProgress);
    }

    public ResponseEntity<?> getDailyProgress(Integer progressId) {
        Optional<DailyProgress> progressOptional = dailyProgressRepository.findById(progressId);
        if (progressOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy bản ghi tiến độ");
        }
        return ResponseEntity.ok(progressOptional.get());
    }

    public ResponseEntity<?> updateDailyProgress(DailyProgress dailyProgress, Integer progressId) {
        Optional<DailyProgress> existingProgressOptional = dailyProgressRepository.findById(progressId);
        if (existingProgressOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy bản ghi tiến độ");
        }

        DailyProgress existingProgress = existingProgressOptional.get();

        // Cập nhật các trường có thể chỉnh sửa
        existingProgress.setCigarettesSmoked(dailyProgress.getCigarettesSmoked());
        existingProgress.setMoodLevel(dailyProgress.getMoodLevel());
        existingProgress.setCravingIntensity(dailyProgress.getCravingIntensity());
        existingProgress.setNotes(dailyProgress.getNotes());

        // Cập nhật health score và money saved
        BigDecimal pricePerPack = existingProgress.getPlan().getSmokingHistory().getPricePerPack();
        int cigarettesPerDay = existingProgress.getPlan().getSmokingHistory().getCigarettesPerDay();
        int cigarettesSmoked = dailyProgress.getCigarettesSmoked();

        BigDecimal moneySaved = pricePerPack.multiply(BigDecimal.valueOf((cigarettesPerDay - cigarettesSmoked) / 20.0));
        existingProgress.setMoneySaved(moneySaved);

        int healthScore = calculateHealthScore(cigarettesSmoked, cigarettesPerDay, dailyProgress.getCravingIntensity());
        existingProgress.setHealthScore(healthScore);

        DailyProgress updatedProgress = dailyProgressRepository.save(existingProgress);
        return ResponseEntity.ok(updatedProgress);
    }

    public List<DailyProgress> getProgressByDateRange(Integer userId, LocalDate startDate, LocalDate endDate) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            return List.of();
        }
        return dailyProgressRepository.findByUserAndDateBetween(user.get(), startDate, endDate);
    }

    public List<DailyProgress> getPlanProgress(Integer planId) {
        Optional<QuitPlan> plan = quitPlanRepository.findById(planId);
        return plan.map(value -> dailyProgressRepository.findByPlan(value)).orElse(List.of());
    }

    // Phương thức tính điểm sức khỏe dựa trên số điếu thuốc hút và mức độ thèm thuốc
    private int calculateHealthScore(int cigarettesSmoked, int baselineCigarettes, int cravingIntensity) {
        // Công thức tính điểm:
        // - Điểm cơ bản: 50
        // - Mỗi điếu không hút so với mức cơ bản: +2 điểm
        // - Điểm trừ dựa trên cường độ thèm thuốc (1-5): -2 điểm cho mỗi cấp độ thèm thuốc

        int baseScore = 50;
        int cigarettesReduction = Math.max(0, baselineCigarettes - cigarettesSmoked);
        int reductionBonus = cigarettesReduction * 2;
        int cravingPenalty = cravingIntensity * 2;

        return Math.min(100, Math.max(0, baseScore + reductionBonus - cravingPenalty));
    }
}
