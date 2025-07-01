package edu.fpt.smokingcessionnew.service;

import edu.fpt.smokingcessionnew.dto.*;
import edu.fpt.smokingcessionnew.entity.QuitPlan;
import edu.fpt.smokingcessionnew.entity.SmokingHistory;
import edu.fpt.smokingcessionnew.entity.User;
import edu.fpt.smokingcessionnew.entity.CoachingSession;
import edu.fpt.smokingcessionnew.repository.QuitPlanRepository;
import edu.fpt.smokingcessionnew.repository.SmokingHistoryRepository;
import edu.fpt.smokingcessionnew.repository.UserRepository;
import edu.fpt.smokingcessionnew.repository.CoachingSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class QuitPlanService {

    @Autowired
    private QuitPlanRepository quitPlanRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SmokingHistoryRepository smokingHistoryRepository;

    @Autowired
    private CoachingSessionRepository coachingSessionRepository;

    @Autowired
    private EmailService emailService;

    // Quit Plan Status Constants
    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PENDING_COACH_REVIEW = 1;
    private static final int STATUS_COACH_CUSTOMIZED = 2;
    private static final int STATUS_ACTIVE = 3;
    private static final int STATUS_COMPLETED = 4;
    private static final int STATUS_PAUSED = 5;

    // Session Type Constants
    private static final int SESSION_TYPE_CONSULTATION = 1;
    private static final int SESSION_TYPE_FOLLOW_UP = 2;
    private static final int SESSION_TYPE_EMERGENCY = 3;

    // Session Status Constants
    private static final int SESSION_STATUS_PENDING = 1;
    private static final int SESSION_STATUS_COMPLETED = 2;
    private static final int SESSION_STATUS_CANCELLED = 3;

    public ResponseEntity<?> createQuitPlan(QuitPlanRequest request) {
        try {
            // Validate smoking history
            Optional<SmokingHistory> smokingHistoryOpt = smokingHistoryRepository.findById(request.getSmokingHistoryId());
            if (smokingHistoryOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Không tìm thấy lịch sử hút thuốc"));
            }

            SmokingHistory smokingHistory = smokingHistoryOpt.get();
            User user = smokingHistory.getUser();

            // Check if user has active membership
            if (!hasActiveMembership(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Bạn cần mua gói membership để sử dụng tính năng này"));
            }

            // Create quit plan
            QuitPlan quitPlan = new QuitPlan();
            quitPlan.setUser(user);
            quitPlan.setSmokingHistory(smokingHistory);
            quitPlan.setTargetQuitDate(request.getTargetQuitDate());
            quitPlan.setQuitMethod(request.getQuitMethod());
            quitPlan.setDailyReductionTarget(request.getDailyReductionTarget());
            quitPlan.setMilestoneGoals(request.getPersonalGoals());
            quitPlan.setPersonalizedStrategies(request.getPreferredStrategies());
            quitPlan.setStatus(STATUS_DRAFT);
            quitPlan.setCreatedDate(LocalDateTime.now());

            // Auto-request coaching if user didn't select specific coach
            if (request.getCoachId() == null) {
                User assignedCoach = autoAssignCoach();
                if (assignedCoach != null) {
                    quitPlan.setCoach(assignedCoach);
                }
            } else {
                Optional<User> coachOpt = userRepository.findById(request.getCoachId());
                if (coachOpt.isPresent()) {
                    quitPlan.setCoach(coachOpt.get());
                }
            }

            QuitPlan savedPlan = quitPlanRepository.save(quitPlan);

            QuitPlanResponse response = convertToResponse(savedPlan);

            return ResponseEntity.ok(Map.of(
                "message", "Tạo kế hoạch cai thuốc thành công. Yêu cầu tư vấn đã được gửi đến coach.",
                "data", response
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    public ResponseEntity<?> requestCoaching(CoachingRequestDto request) {
        try {
            Optional<QuitPlan> quitPlanOpt = quitPlanRepository.findById(request.getQuitPlanId());
            if (quitPlanOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Không tìm thấy kế hoạch cai thuốc"));
            }

            QuitPlan quitPlan = quitPlanOpt.get();

            // Update status to pending coach review
            quitPlan.setStatus(STATUS_PENDING_COACH_REVIEW);
            quitPlanRepository.save(quitPlan);

            // Create coaching session request
            CoachingSession coachingSession = new CoachingSession();
            coachingSession.setMember(quitPlan.getUser());

            // Assign coach
            User coach = assignBestCoach(request);
            coachingSession.setCoach(coach);

            coachingSession.setSessionType(SESSION_TYPE_CONSULTATION);
            coachingSession.setStatus(SESSION_STATUS_PENDING);
            coachingSession.setNotes(request.getRequestMessage());
            coachingSession.setCreatedDate(LocalDateTime.now());

            coachingSessionRepository.save(coachingSession);

            // Send notification email to coach
            sendCoachNotificationEmail(coach, quitPlan, request);

            return ResponseEntity.ok(Map.of(
                "message", "Yêu cầu tư vấn đã được gửi thành công. Coach " + coach.getFullName() + " sẽ xem xét và phản hồi sớm.",
                "coachName", coach.getFullName(),
                "estimatedResponseTime", "24-48 giờ"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    public ResponseEntity<List<QuitPlanResponse>> getUserQuitPlans(Integer userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<QuitPlan> quitPlans = quitPlanRepository.findByUser(userOpt.get());
        List<QuitPlanResponse> responses = quitPlans.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    public ResponseEntity<QuitPlanResponse> getQuitPlanById(Integer id) {
        Optional<QuitPlan> quitPlanOpt = quitPlanRepository.findById(id);
        if (quitPlanOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        QuitPlanResponse response = convertToResponse(quitPlanOpt.get());
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> getPendingCoachRequests(Integer coachId) {
        try {
            // Get all pending coaching sessions for this coach
            List<CoachingSession> pendingSessions = coachingSessionRepository
                    .findByCoachIdAndStatus(coachId, SESSION_STATUS_PENDING);

            List<Map<String, Object>> requests = pendingSessions.stream()
                    .map(this::convertCoachingSessionToMap)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                "message", "Danh sách yêu cầu tư vấn đang chờ xử lý",
                "data", requests,
                "total", requests.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    public ResponseEntity<?> getQuitPlanForCoachReview(Integer quitPlanId) {
        try {
            Optional<QuitPlan> quitPlanOpt = quitPlanRepository.findById(quitPlanId);
            if (quitPlanOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Không tìm thấy kế hoạch cai thuốc"));
            }

            QuitPlan quitPlan = quitPlanOpt.get();
            SmokingHistory smokingHistory = quitPlan.getSmokingHistory();

            Map<String, Object> reviewData = new HashMap<>();
            reviewData.put("quitPlan", convertToResponse(quitPlan));
            reviewData.put("smokingHistory", convertSmokingHistoryForCoach(smokingHistory));
            reviewData.put("userProfile", convertUserProfileForCoach(quitPlan.getUser()));
            reviewData.put("riskAssessment", calculateRiskAssessment(smokingHistory));
            reviewData.put("recommendations", generateInitialRecommendations(smokingHistory));

            return ResponseEntity.ok(Map.of(
                "message", "Thông tin để coach tùy chỉnh kế hoạch",
                "data", reviewData
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    public ResponseEntity<?> coachCustomizePlan(CoachCustomizationDto request) {
        try {
            Optional<QuitPlan> quitPlanOpt = quitPlanRepository.findById(request.getQuitPlanId());
            if (quitPlanOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Không tìm thấy kế hoạch cai thuốc"));
            }

            QuitPlan quitPlan = quitPlanOpt.get();

            // Update quit plan with coach customizations
            if (request.getRevisedTargetQuitDate() != null) {
                quitPlan.setTargetQuitDate(request.getRevisedTargetQuitDate());
            }
            if (request.getRecommendedQuitMethod() != null) {
                quitPlan.setQuitMethod(request.getRecommendedQuitMethod());
            }
            if (request.getRecommendedDailyReduction() != null) {
                quitPlan.setDailyReductionTarget(request.getRecommendedDailyReduction());
            }
            if (request.getCustomizedMilestoneGoals() != null) {
                quitPlan.setMilestoneGoals(request.getCustomizedMilestoneGoals());
            }
            if (request.getPersonalizedStrategies() != null) {
                quitPlan.setPersonalizedStrategies(request.getPersonalizedStrategies());
            }

            // Update status to coach customized
            quitPlan.setStatus(STATUS_COACH_CUSTOMIZED);

            QuitPlan savedPlan = quitPlanRepository.save(quitPlan);

            // Send notification to user
            sendUserNotificationEmail(quitPlan.getUser(), quitPlan, request);

            QuitPlanResponse response = convertToResponse(savedPlan);

            return ResponseEntity.ok(Map.of(
                "message", "Kế hoạch đã được tùy chỉnh thành công. Thông báo đã được gửi đến người dùng.",
                "data", response
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    public ResponseEntity<?> activateQuitPlan(Integer id, Integer userId) {
        try {
            Optional<QuitPlan> quitPlanOpt = quitPlanRepository.findById(id);
            if (quitPlanOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Không tìm thấy kế hoạch cai thuốc"));
            }

            QuitPlan quitPlan = quitPlanOpt.get();

            // Verify ownership
            if (!quitPlan.getUser().getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Bạn không có quyền kích hoạt kế hoạch này"));
            }

            // Check if plan is ready for activation
            if (quitPlan.getStatus() != STATUS_COACH_CUSTOMIZED) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Kế hoạch chưa được coach tùy chỉnh hoặc đã được kích hoạt"));
            }

            // Activate the plan
            quitPlan.setStatus(STATUS_ACTIVE);
            quitPlanRepository.save(quitPlan);

            return ResponseEntity.ok(Map.of(
                "message", "Kế hoạch cai thuốc đã được kích hoạt thành công! Chúc bạn thành công!",
                "data", convertToResponse(quitPlan),
                "nextSteps", Map.of(
                    "startDate", quitPlan.getTargetQuitDate(),
                    "firstMilestone", "Ngày đầu tiên không hút thuốc",
                    "supportContact", "Liên hệ coach khi cần hỗ trợ"
                )
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    public ResponseEntity<?> getCoachManagedPlans(Integer coachId) {
        try {
            // Verify coach exists and has correct role
            Optional<User> coachOpt = userRepository.findById(coachId);
            if (coachOpt.isEmpty() || coachOpt.get().getRole() != 3) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Không tìm thấy coach"));
            }

            // Get all plans managed by this coach
            List<QuitPlan> managedPlans = quitPlanRepository.findByCoach(coachOpt.get());

            List<QuitPlanResponse> responses = managedPlans.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                "message", "Danh sách kế hoạch đang quản lý",
                "data", responses,
                "total", responses.size(),
                "coachName", coachOpt.get().getFullName()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    public ResponseEntity<?> getCoachPlansByStatus(Integer coachId, String status) {
        try {
            // Verify coach exists
            Optional<User> coachOpt = userRepository.findById(coachId);
            if (coachOpt.isEmpty() || coachOpt.get().getRole() != 3) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Không tìm thấy coach"));
            }

            // Convert status string to integer
            Integer statusCode = getStatusCodeFromString(status);
            if (statusCode == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Trạng thái không hợp lệ. Sử dụng: DRAFT, PENDING_COACH_REVIEW, COACH_CUSTOMIZED, ACTIVE, COMPLETED, PAUSED"));
            }

            // Get plans by coach and status
            List<QuitPlan> filteredPlans = quitPlanRepository.findByCoachAndStatus(coachOpt.get(), statusCode);

            List<QuitPlanResponse> responses = filteredPlans.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                "message", "Danh sách kế hoạch với trạng thái " + status,
                "data", responses,
                "total", responses.size(),
                "status", status,
                "coachName", coachOpt.get().getFullName()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    // Helper method to convert status string to code
    private Integer getStatusCodeFromString(String status) {
        switch (status.toUpperCase()) {
            case "DRAFT": return STATUS_DRAFT;
            case "PENDING_COACH_REVIEW": return STATUS_PENDING_COACH_REVIEW;
            case "COACH_CUSTOMIZED": return STATUS_COACH_CUSTOMIZED;
            case "ACTIVE": return STATUS_ACTIVE;
            case "COMPLETED": return STATUS_COMPLETED;
            case "PAUSED": return STATUS_PAUSED;
            default: return null;
        }
    }

    // Helper methods
    private boolean hasActiveMembership(Integer userId) {
        // TODO: Implement membership check logic
        // Check if user has active membership subscription
        return true; // Temporary return true for testing
    }

    private User autoAssignCoach() {
        // TODO: Implement auto coach assignment logic
        // Find available coach with least workload or best match
        List<User> availableCoaches = userRepository.findByRole(3); // Role 3 = COACH
        return availableCoaches.isEmpty() ? null : availableCoaches.get(0);
    }

    private User assignBestCoach(CoachingRequestDto request) {
        if (request.getPreferredCoachId() != null) {
            Optional<User> preferredCoach = userRepository.findById(request.getPreferredCoachId());
            if (preferredCoach.isPresent()) {
                return preferredCoach.get();
            }
        }

        // Auto assign best available coach
        List<User> availableCoaches = userRepository.findByRole(3); // Role 3 = COACH
        return availableCoaches.isEmpty() ? null : availableCoaches.get(0);
    }

    private void sendCoachNotificationEmail(User coach, QuitPlan quitPlan, CoachingRequestDto request) {
        try {
            String subject = "Yêu cầu tư vấn mới từ " + quitPlan.getUser().getFullName();
            String content = "Bạn có một yêu cầu tư vấn mới từ thành viên " + quitPlan.getUser().getFullName() +
                           ". Vui lòng đăng nhập để xem chi tiết và tùy chỉnh kế hoạch cai thuốc.";
            emailService.sendSimpleEmail(coach.getEmail(), subject, content);
        } catch (Exception e) {
            // Log error but don't fail the main process
            System.err.println("Failed to send email notification to coach: " + e.getMessage());
        }
    }

    private void sendUserNotificationEmail(User user, QuitPlan quitPlan, CoachCustomizationDto customization) {
        try {
            String subject = "Kế hoạch cai thuốc của bạn đã được tùy chỉnh";
            String content = "Coach đã tùy chỉnh kế hoạch cai thuốc của bạn. " +
                           "Vui lòng đăng nhập để xem chi tiết và kích hoạt kế hoạch.";
            emailService.sendSimpleEmail(user.getEmail(), subject, content);
        } catch (Exception e) {
            // Log error but don't fail the main process
            System.err.println("Failed to send email notification to user: " + e.getMessage());
        }
    }

    private QuitPlanResponse convertToResponse(QuitPlan quitPlan) {
        QuitPlanResponse response = new QuitPlanResponse();
        response.setId(quitPlan.getId());
        response.setUserId(quitPlan.getUser().getId());
        response.setUserFullName(quitPlan.getUser().getFullName());
        response.setSmokingHistoryId(quitPlan.getSmokingHistory().getId());

        // Thêm thông tin coach
        if (quitPlan.getCoach() != null) {
            response.setCoachId(quitPlan.getCoach().getId());
            response.setCoachName(quitPlan.getCoach().getFullName());
        }

        response.setTargetQuitDate(quitPlan.getTargetQuitDate());
        response.setQuitMethod(quitPlan.getQuitMethod());
        response.setDailyReductionTarget(quitPlan.getDailyReductionTarget());
        response.setMilestoneGoals(quitPlan.getMilestoneGoals());
        response.setPersonalizedStrategies(quitPlan.getPersonalizedStrategies());
        response.setCreatedDate(quitPlan.getCreatedDate());
        response.setStatus(getStatusString(quitPlan.getStatus()));
        response.setStatusDescription(getStatusDescription(quitPlan.getStatus()));

        return response;
    }

    private String getStatusString(Integer status) {
        switch (status) {
            case STATUS_DRAFT: return "DRAFT";
            case STATUS_PENDING_COACH_REVIEW: return "PENDING_COACH_REVIEW";
            case STATUS_COACH_CUSTOMIZED: return "COACH_CUSTOMIZED";
            case STATUS_ACTIVE: return "ACTIVE";
            case STATUS_COMPLETED: return "COMPLETED";
            case STATUS_PAUSED: return "PAUSED";
            default: return "UNKNOWN";
        }
    }

    private String getStatusDescription(Integer status) {
        switch (status) {
            case STATUS_DRAFT: return "Bản nháp - chưa gửi cho coach";
            case STATUS_PENDING_COACH_REVIEW: return "Đang chờ coach xem xét";
            case STATUS_COACH_CUSTOMIZED: return "Coach đã tùy chỉnh - sẵn sàng kích hoạt";
            case STATUS_ACTIVE: return "Đang thực hiện";
            case STATUS_COMPLETED: return "Hoàn thành";
            case STATUS_PAUSED: return "Tạm dừng";
            default: return "Không xác định";
        }
    }

    // Additional helper methods for coaching workflow
    private Map<String, Object> convertCoachingSessionToMap(CoachingSession session) {
        // Implementation for converting coaching session to map
        return new HashMap<>();
    }

    private Map<String, Object> convertSmokingHistoryForCoach(SmokingHistory history) {
        // Implementation for converting smoking history for coach review
        return new HashMap<>();
    }

    private Map<String, Object> convertUserProfileForCoach(User user) {
        // Implementation for converting user profile for coach
        return new HashMap<>();
    }

    private Map<String, Object> calculateRiskAssessment(SmokingHistory history) {
        // Implementation for risk assessment
        return new HashMap<>();
    }

    private Map<String, Object> generateInitialRecommendations(SmokingHistory history) {
        // Implementation for generating recommendations
        return new HashMap<>();
    }

    // Placeholder methods for additional endpoints
    public ResponseEntity<?> pauseQuitPlan(Integer id, Integer userId, String reason) {
        // Implementation for pausing quit plan
        return ResponseEntity.ok(Map.of("message", "Feature coming soon"));
    }

    public ResponseEntity<?> resumeQuitPlan(Integer id, Integer userId) {
        // Implementation for resuming quit plan
        return ResponseEntity.ok(Map.of("message", "Feature coming soon"));
    }

    public ResponseEntity<?> getQuitPlanProgress(Integer id) {
        // Implementation for getting quit plan progress
        return ResponseEntity.ok(Map.of("message", "Feature coming soon"));
    }

    public ResponseEntity<?> getAvailableCoaches() {
        // Implementation for getting available coaches
        return ResponseEntity.ok(Map.of("message", "Feature coming soon"));
    }

    public ResponseEntity<?> assignCoach(Integer userId, Integer coachId, Integer quitPlanId) {
        // Implementation for manually assigning coach
        return ResponseEntity.ok(Map.of("message", "Feature coming soon"));
    }
}
