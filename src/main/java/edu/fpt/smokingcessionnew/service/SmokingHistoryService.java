package edu.fpt.smokingcessionnew.service;

import edu.fpt.smokingcessionnew.dto.SmokingHistoryRequest;
import edu.fpt.smokingcessionnew.dto.SmokingHistoryResponse;
import edu.fpt.smokingcessionnew.entity.SmokingHistory;
import edu.fpt.smokingcessionnew.entity.User;
import edu.fpt.smokingcessionnew.repository.SmokingHistoryRepository;
import edu.fpt.smokingcessionnew.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SmokingHistoryService {

    @Autowired
    private SmokingHistoryRepository smokingHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    public ResponseEntity<?> recordSmokingHistory(SmokingHistoryRequest request, Integer userId) {
        try {
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Người dùng không tồn tại"));
            }

            User user = userOptional.get();
            SmokingHistory smokingHistory = convertToEntity(request);
            smokingHistory.setUser(user);
            smokingHistory.setRecordedDate(LocalDate.now());
            smokingHistory.setUpdatedDate(LocalDate.now());

            SmokingHistory savedHistory = smokingHistoryRepository.save(smokingHistory);
            SmokingHistoryResponse response = convertToResponse(savedHistory);

            return ResponseEntity.ok(Map.of(
                "message", "Ghi nhận lịch sử hút thuốc thành công",
                "data", response
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    public ResponseEntity<List<SmokingHistoryResponse>> getUserSmokingHistory(Integer userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<SmokingHistory> histories = smokingHistoryRepository.findByUser(userOptional.get());
        List<SmokingHistoryResponse> responses = histories.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    public ResponseEntity<SmokingHistoryResponse> getLatestSmokingHistory(Integer userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Optional<SmokingHistory> latestHistory = smokingHistoryRepository
                .findTopByUserOrderByRecordedDateDesc(userOptional.get());

        if (latestHistory.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        SmokingHistoryResponse response = convertToResponse(latestHistory.get());
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<SmokingHistoryResponse> getSmokingHistoryById(Integer id) {
        Optional<SmokingHistory> historyOptional = smokingHistoryRepository.findById(id);
        if (historyOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        SmokingHistoryResponse response = convertToResponse(historyOptional.get());
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> updateSmokingHistory(Integer id, SmokingHistoryRequest request) {
        try {
            Optional<SmokingHistory> existingHistoryOptional = smokingHistoryRepository.findById(id);
            if (existingHistoryOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Không tìm thấy lịch sử hút thuốc"));
            }

            SmokingHistory existingHistory = existingHistoryOptional.get();
            updateEntityFromRequest(existingHistory, request);
            existingHistory.setUpdatedDate(LocalDate.now());

            SmokingHistory updatedHistory = smokingHistoryRepository.save(existingHistory);
            SmokingHistoryResponse response = convertToResponse(updatedHistory);

            return ResponseEntity.ok(Map.of(
                "message", "Cập nhật lịch sử hút thuốc thành công",
                "data", response
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    public ResponseEntity<?> deleteSmokingHistory(Integer id) {
        try {
            Optional<SmokingHistory> historyOptional = smokingHistoryRepository.findById(id);
            if (historyOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Không tìm thấy lịch sử hút thuốc"));
            }

            smokingHistoryRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Xóa lịch sử hút thuốc thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    public ResponseEntity<?> getSmokingStatistics(Integer userId) {
        try {
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Người dùng không tồn tại"));
            }

            Optional<SmokingHistory> latestHistoryOpt = smokingHistoryRepository
                    .findTopByUserOrderByRecordedDateDesc(userOptional.get());

            if (latestHistoryOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Người dùng chưa có lịch sử hút thuốc"));
            }

            SmokingHistory history = latestHistoryOpt.get();
            Map<String, Object> statistics = calculateDetailedStatistics(history);

            return ResponseEntity.ok(Map.of(
                "message", "Lấy thống kê thành công",
                "data", statistics
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    // Helper methods
    private SmokingHistory convertToEntity(SmokingHistoryRequest request) {
        SmokingHistory entity = new SmokingHistory();
        entity.setCigarettesPerDay(request.getCigarettesPerDay());
        entity.setPricePerPack(request.getPricePerPack());
        entity.setDurationYears(request.getDurationYears());
        entity.setAgeStartedSmoking(request.getAgeStartedSmoking());
        entity.setBrandPreference(request.getBrandPreference());
        entity.setTriggers(request.getTriggers());
        entity.setPreviousQuitAttempts(request.getPreviousQuitAttempts());
        entity.setLongestQuitPeriodDays(request.getLongestQuitPeriodDays());
        entity.setMotivationLevel(request.getMotivationLevel());
        entity.setHealthConditionsRelated(request.getHealthConcerns());
        // Note: Entity doesn't have notes field, so we skip it
        return entity;
    }

    private void updateEntityFromRequest(SmokingHistory entity, SmokingHistoryRequest request) {
        entity.setCigarettesPerDay(request.getCigarettesPerDay());
        entity.setPricePerPack(request.getPricePerPack());
        entity.setDurationYears(request.getDurationYears());
        entity.setAgeStartedSmoking(request.getAgeStartedSmoking());
        entity.setBrandPreference(request.getBrandPreference());
        entity.setTriggers(request.getTriggers());
        entity.setPreviousQuitAttempts(request.getPreviousQuitAttempts());
        entity.setLongestQuitPeriodDays(request.getLongestQuitPeriodDays());
        entity.setMotivationLevel(request.getMotivationLevel());
        entity.setHealthConditionsRelated(request.getHealthConcerns());
        // Note: Entity doesn't have notes field, so we skip it
    }

    private SmokingHistoryResponse convertToResponse(SmokingHistory entity) {
        SmokingHistoryResponse response = new SmokingHistoryResponse();
        response.setId(entity.getId());
        response.setUserId(entity.getUser().getId());
        response.setUserFullName(entity.getUser().getFullName());
        response.setCigarettesPerDay(entity.getCigarettesPerDay());
        response.setPricePerPack(entity.getPricePerPack());
        response.setDurationYears(entity.getDurationYears());
        response.setAgeStartedSmoking(entity.getAgeStartedSmoking());
        response.setBrandPreference(entity.getBrandPreference());
        response.setTriggers(entity.getTriggers());
        response.setPreviousQuitAttempts(entity.getPreviousQuitAttempts());
        response.setLongestQuitPeriodDays(entity.getLongestQuitPeriodDays());
        response.setMotivationLevel(entity.getMotivationLevel());
        response.setHealthConcerns(entity.getHealthConditionsRelated());
        response.setNotes(null); // Entity doesn't have notes field
        response.setRecordedDate(entity.getRecordedDate());

        // Calculate derived fields
        if (entity.getCigarettesPerDay() != null && entity.getPricePerPack() != null) {
            // Assuming 20 cigarettes per pack
            BigDecimal cigarettesPerPack = new BigDecimal("20");
            BigDecimal dailyCost = entity.getPricePerPack()
                    .multiply(new BigDecimal(entity.getCigarettesPerDay()))
                    .divide(cigarettesPerPack, 2, RoundingMode.HALF_UP);

            response.setDailyCost(dailyCost);
            response.setMonthlyCost(dailyCost.multiply(new BigDecimal("30")));
            response.setYearlyCost(dailyCost.multiply(new BigDecimal("365")));
        }

        if (entity.getCigarettesPerDay() != null && entity.getDurationYears() != null) {
            int totalCigarettes = entity.getCigarettesPerDay() * entity.getDurationYears() * 365;
            response.setTotalCigarettesSmoked(totalCigarettes);
        }

        response.setRiskLevel(calculateRiskLevel(entity));

        return response;
    }

    private String calculateRiskLevel(SmokingHistory history) {
        if (history.getCigarettesPerDay() == null || history.getDurationYears() == null) {
            return "Unknown";
        }

        int cigarettesPerDay = history.getCigarettesPerDay();
        int durationYears = history.getDurationYears();

        if (cigarettesPerDay >= 20 || durationYears >= 10) {
            return "Very High";
        } else if (cigarettesPerDay >= 10 || durationYears >= 5) {
            return "High";
        } else if (cigarettesPerDay >= 5 || durationYears >= 2) {
            return "Medium";
        } else {
            return "Low";
        }
    }

    private Map<String, Object> calculateDetailedStatistics(SmokingHistory history) {
        Map<String, Object> stats = new HashMap<>();

        if (history.getCigarettesPerDay() != null && history.getPricePerPack() != null) {
            BigDecimal cigarettesPerPack = new BigDecimal("20");
            BigDecimal dailyCost = history.getPricePerPack()
                    .multiply(new BigDecimal(history.getCigarettesPerDay()))
                    .divide(cigarettesPerPack, 2, RoundingMode.HALF_UP);

            stats.put("dailyCost", dailyCost);
            stats.put("weeklyCost", dailyCost.multiply(new BigDecimal("7")));
            stats.put("monthlyCost", dailyCost.multiply(new BigDecimal("30")));
            stats.put("yearlyCost", dailyCost.multiply(new BigDecimal("365")));
        }

        if (history.getCigarettesPerDay() != null && history.getDurationYears() != null) {
            int totalCigarettes = history.getCigarettesPerDay() * history.getDurationYears() * 365;
            stats.put("totalCigarettesSmoked", totalCigarettes);

            // Estimate time spent smoking (assuming 5 minutes per cigarette)
            int totalMinutesSmoked = totalCigarettes * 5;
            int totalHoursSmoked = totalMinutesSmoked / 60;
            int totalDaysSmoked = totalHoursSmoked / 24;

            stats.put("timeSpentSmoking", Map.of(
                "totalMinutes", totalMinutesSmoked,
                "totalHours", totalHoursSmoked,
                "totalDays", totalDaysSmoked
            ));
        }

        stats.put("riskLevel", calculateRiskLevel(history));
        stats.put("motivationLevel", history.getMotivationLevel());
        stats.put("previousQuitAttempts", history.getPreviousQuitAttempts());
        stats.put("longestQuitPeriodDays", history.getLongestQuitPeriodDays());

        return stats;
    }
}
