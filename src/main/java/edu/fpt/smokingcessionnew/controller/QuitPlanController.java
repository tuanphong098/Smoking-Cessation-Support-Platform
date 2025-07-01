package edu.fpt.smokingcessionnew.controller;

import edu.fpt.smokingcessionnew.dto.*;
import edu.fpt.smokingcessionnew.service.QuitPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quit-plan")
@Tag(name = "Quit Plan Management", description = "APIs quản lý kế hoạch cai thuốc và coaching workflow")
@SecurityRequirement(name = "bearerAuth")
public class QuitPlanController {

    @Autowired
    private QuitPlanService quitPlanService;

    @PostMapping
    @Operation(summary = "Tạo kế hoạch cai thuốc", description = "User tạo kế hoạch cai thuốc mới và gửi yêu cầu tư vấn đến coach")
    @ApiResponse(responseCode = "200", description = "Tạo kế hoạch thành công")
    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    @ApiResponse(responseCode = "404", description = "Không tìm thấy smoking history hoặc user")
    public ResponseEntity<?> createQuitPlan(@Valid @RequestBody QuitPlanRequest request) {
        return quitPlanService.createQuitPlan(request);
    }

    @PostMapping("/request-coaching")
    @Operation(summary = "Gửi yêu cầu tư vấn đến coach", description = "User gửi kế hoạch và smoking history đến coach để tùy chỉnh")
    @ApiResponse(responseCode = "200", description = "Gửi yêu cầu thành công")
    public ResponseEntity<?> requestCoaching(@Valid @RequestBody CoachingRequestDto request) {
        return quitPlanService.requestCoaching(request);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Lấy danh sách kế hoạch của user", description = "Lấy tất cả kế hoạch cai thuốc của một user")
    public ResponseEntity<List<QuitPlanResponse>> getUserQuitPlans(
            @Parameter(description = "ID người dùng") @PathVariable Integer userId) {
        return quitPlanService.getUserQuitPlans(userId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết kế hoạch", description = "Lấy thông tin chi tiết của một kế hoạch cai thuốc")
    public ResponseEntity<QuitPlanResponse> getQuitPlanById(
            @Parameter(description = "ID kế hoạch") @PathVariable Integer id) {
        return quitPlanService.getQuitPlanById(id);
    }

    // APIs dành cho Coach
    @GetMapping("/coach/{coachId}/pending")
    @Operation(summary = "Lấy danh sách yêu cầu đang chờ coach xử lý", description = "Coach xem các yêu cầu tư vấn đang chờ")
    public ResponseEntity<?> getPendingCoachRequests(
            @Parameter(description = "ID coach") @PathVariable Integer coachId) {
        return quitPlanService.getPendingCoachRequests(coachId);
    }

    @GetMapping("/coach-review/{quitPlanId}")
    @Operation(summary = "Coach xem chi tiết để tùy chỉnh", description = "Coach xem smoking history và quit plan để tùy chỉnh")
    public ResponseEntity<?> getQuitPlanForCoachReview(
            @Parameter(description = "ID kế hoạch") @PathVariable Integer quitPlanId) {
        return quitPlanService.getQuitPlanForCoachReview(quitPlanId);
    }

    @PostMapping("/coach-customize")
    @Operation(summary = "Coach tùy chỉnh kế hoạch", description = "Coach tùy chỉnh kế hoạch dựa trên smoking history và gửi về cho user")
    @ApiResponse(responseCode = "200", description = "Tùy chỉnh thành công")
    public ResponseEntity<?> coachCustomizePlan(@Valid @RequestBody CoachCustomizationDto request) {
        return quitPlanService.coachCustomizePlan(request);
    }

    @PutMapping("/{id}/activate")
    @Operation(summary = "Kích hoạt kế hoạch", description = "User xem kế hoạch đã được coach tùy chỉnh và quyết định kích hoạt")
    @ApiResponse(responseCode = "200", description = "Kích hoạt thành công")
    public ResponseEntity<?> activateQuitPlan(
            @Parameter(description = "ID kế hoạch") @PathVariable Integer id,
            @Parameter(description = "ID người dùng") @RequestParam Integer userId) {
        return quitPlanService.activateQuitPlan(id, userId);
    }

    @PutMapping("/{id}/pause")
    @Operation(summary = "Tạm dừng kế hoạch", description = "Tạm dừng kế hoạch đang thực hiện")
    public ResponseEntity<?> pauseQuitPlan(
            @Parameter(description = "ID kế hoạch") @PathVariable Integer id,
            @Parameter(description = "ID người dùng") @RequestParam Integer userId,
            @RequestParam(required = false) String reason) {
        return quitPlanService.pauseQuitPlan(id, userId, reason);
    }

    @PutMapping("/{id}/resume")
    @Operation(summary = "Tiếp tục kế hoạch", description = "Tiếp tục kế hoạch đã tạm dừng")
    public ResponseEntity<?> resumeQuitPlan(
            @Parameter(description = "ID kế hoạch") @PathVariable Integer id,
            @Parameter(description = "ID người dùng") @RequestParam Integer userId) {
        return quitPlanService.resumeQuitPlan(id, userId);
    }

    @GetMapping("/{id}/progress")
    @Operation(summary = "Xem tiến trình thực hiện", description = "Xem tiến trình thực hiện kế hoạch cai thuốc")
    public ResponseEntity<?> getQuitPlanProgress(
            @Parameter(description = "ID kế hoạch") @PathVariable Integer id) {
        return quitPlanService.getQuitPlanProgress(id);
    }

    // APIs quản lý coach
    @GetMapping("/coaches/available")
    @Operation(summary = "Lấy danh sách coach khả dụng", description = "Lấy danh sách các coach đang hoạt động")
    public ResponseEntity<?> getAvailableCoaches() {
        return quitPlanService.getAvailableCoaches();
    }

    @PostMapping("/assign-coach")
    @Operation(summary = "Gán coach cho user", description = "Admin hoặc hệ thống tự động gán coach cho user")
    public ResponseEntity<?> assignCoach(
            @RequestParam Integer userId,
            @RequestParam Integer coachId,
            @RequestParam Integer quitPlanId) {
        return quitPlanService.assignCoach(userId, coachId, quitPlanId);
    }

    @GetMapping("/coach/{coachId}/plans")
    @Operation(summary = "Coach xem tất cả kế hoạch đang quản lý", description = "Coach xem danh sách tất cả quit plans mà họ đang quản lý")
    public ResponseEntity<?> getCoachManagedPlans(
            @Parameter(description = "ID coach") @PathVariable Integer coachId) {
        return quitPlanService.getCoachManagedPlans(coachId);
    }

    @GetMapping("/coach/{coachId}/plans/status/{status}")
    @Operation(summary = "Coach xem kế hoạch theo trạng thái", description = "Coach xem các kế hoạch theo trạng thái cụ thể")
    public ResponseEntity<?> getCoachPlansByStatus(
            @Parameter(description = "ID coach") @PathVariable Integer coachId,
            @Parameter(description = "Trạng thái kế hoạch") @PathVariable String status) {
        return quitPlanService.getCoachPlansByStatus(coachId, status);
    }
}
