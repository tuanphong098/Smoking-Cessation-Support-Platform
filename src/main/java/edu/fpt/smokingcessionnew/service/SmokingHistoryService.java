package edu.fpt.smokingcessionnew.service;

import edu.fpt.smokingcessionnew.entity.SmokingHistory;
import edu.fpt.smokingcessionnew.entity.User;
import edu.fpt.smokingcessionnew.repository.SmokingHistoryRepository;
import edu.fpt.smokingcessionnew.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class SmokingHistoryService {

    @Autowired
    private SmokingHistoryRepository smokingHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    public ResponseEntity<?> recordSmokingHistory(SmokingHistory smokingHistory, Integer userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Người dùng không tồn tại");
        }

        User user = userOptional.get();
        smokingHistory.setUser(user);
        smokingHistory.setRecordedDate(LocalDate.now());
        smokingHistory.setUpdatedDate(LocalDate.now());

        SmokingHistory savedHistory = smokingHistoryRepository.save(smokingHistory);
        return ResponseEntity.ok(savedHistory);
    }

    public ResponseEntity<?> updateSmokingHistory(SmokingHistory smokingHistory, Integer historyId) {
        Optional<SmokingHistory> existingHistoryOptional = smokingHistoryRepository.findById(historyId);
        if (existingHistoryOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy lịch sử hút thuốc");
        }

        SmokingHistory existingHistory = existingHistoryOptional.get();

        // Cập nhật các trường có thể chỉnh sửa
        existingHistory.setCigarettesPerDay(smokingHistory.getCigarettesPerDay());
        existingHistory.setPricePerPack(smokingHistory.getPricePerPack());
        existingHistory.setDurationYears(smokingHistory.getDurationYears());
        existingHistory.setAgeStartedSmoking(smokingHistory.getAgeStartedSmoking());
        existingHistory.setBrandPreference(smokingHistory.getBrandPreference());
        existingHistory.setTriggers(smokingHistory.getTriggers());
        existingHistory.setPreviousQuitAttempts(smokingHistory.getPreviousQuitAttempts());
        existingHistory.setLongestQuitPeriodDays(smokingHistory.getLongestQuitPeriodDays());
        existingHistory.setMotivationLevel(smokingHistory.getMotivationLevel());
        existingHistory.setHealthConditionsRelated(smokingHistory.getHealthConditionsRelated());
        existingHistory.setUpdatedDate(LocalDate.now());

        SmokingHistory updatedHistory = smokingHistoryRepository.save(existingHistory);
        return ResponseEntity.ok(updatedHistory);
    }

    public ResponseEntity<?> getSmokingHistory(Integer historyId) {
        Optional<SmokingHistory> historyOptional = smokingHistoryRepository.findById(historyId);
        if (historyOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy lịch sử hút thuốc");
        }
        return ResponseEntity.ok(historyOptional.get());
    }

    public List<SmokingHistory> getUserSmokingHistory(Integer userId) {
        Optional<User> user = userRepository.findById(userId);
        return user.map(value -> smokingHistoryRepository.findByUser(value)).orElse(List.of());
    }

    public ResponseEntity<?> getUserLatestSmokingHistory(Integer userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Người dùng không tồn tại");
        }

        Optional<SmokingHistory> latestHistory = smokingHistoryRepository.findTopByUserOrderByRecordedDateDesc(userOptional.get());
        if (latestHistory.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Người dùng chưa có lịch sử hút thuốc");
        }

        return ResponseEntity.ok(latestHistory.get());
    }

    // Tính toán các thống kê liên quan đến hút thuốc
    public ResponseEntity<?> calculateSmokingStatistics(Integer userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Người dùng không tồn tại");
        }

        Optional<SmokingHistory> latestHistoryOpt = smokingHistoryRepository.findTopByUserOrderByRecordedDateDesc(userOptional.get());
        if (latestHistoryOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Người dùng chưa có lịch sử hút thuốc");
        }

        SmokingHistory latestHistory = latestHistoryOpt.get();

        // Tính toán các chỉ số thống kê
        int cigarettesPerDay = latestHistory.getCigarettesPerDay();
        int durationYears = latestHistory.getDurationYears();

        // Tổng số điếu thuốc đã hút
        int totalCigarettes = cigarettesPerDay * 365 * durationYears;

        // Thời gian đã mất vào việc hút thuốc (giả sử 5 phút/điếu)
        int timeWastedInMinutes = totalCigarettes * 5;

        // Chi phí đã tiêu cho thuốc lá
        double costSpent = (cigarettesPerDay / 20.0) * latestHistory.getPricePerPack().doubleValue() * 365 * durationYears;

        // Tạo kết quả
        return ResponseEntity.ok(
            new SmokingStatistics(
                totalCigarettes,
                timeWastedInMinutes,
                costSpent
            )
        );
    }

    // Class nội bộ cho thống kê hút thuốc
    public static class SmokingStatistics {
        private int totalCigarettes;
        private int timeWastedInMinutes;
        private double totalCostSpent;

        public SmokingStatistics(int totalCigarettes, int timeWastedInMinutes, double totalCostSpent) {
            this.totalCigarettes = totalCigarettes;
            this.timeWastedInMinutes = timeWastedInMinutes;
            this.totalCostSpent = totalCostSpent;
        }

        // Getters
        public int getTotalCigarettes() {
            return totalCigarettes;
        }

        public int getTimeWastedInMinutes() {
            return timeWastedInMinutes;
        }

        public double getTotalCostSpent() {
            return totalCostSpent;
        }

        // Thời gian lãng phí theo ngày
        public double getTimeWastedInDays() {
            return timeWastedInMinutes / (60.0 * 24);
        }
    }
}
