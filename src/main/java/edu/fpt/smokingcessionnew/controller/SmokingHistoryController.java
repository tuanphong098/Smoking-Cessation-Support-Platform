package edu.fpt.smokingcessionnew.controller;

import edu.fpt.smokingcessionnew.dto.SmokingHistoryRequest;
import edu.fpt.smokingcessionnew.dto.SmokingHistoryResponse;
import edu.fpt.smokingcessionnew.service.SmokingHistoryService;
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
@RequestMapping("/api/smoking-history")
@Tag(name = "Smoking History", description = "APIs quản lý lịch sử hút thuốc")
@SecurityRequirement(name = "bearerAuth")
public class SmokingHistoryController {

    @Autowired
    private SmokingHistoryService smokingHistoryService;

    @PostMapping
    @Operation(summary = "Ghi nhận lịch sử hút thuốc", description = "Tạo mới hoặc cập nhật thông tin lịch sử hút thuốc của người dùng")
    @ApiResponse(responseCode = "200", description = "Ghi nhận thành công")
    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    public ResponseEntity<?> recordSmokingHistory(
            @Valid @RequestBody SmokingHistoryRequest request,
            @Parameter(description = "ID người dùng") @RequestParam Integer userId) {
        return smokingHistoryService.recordSmokingHistory(request, userId);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Lấy lịch sử hút thuốc theo user", description = "Lấy tất cả lịch sử hút thuốc của một người dùng")
    @ApiResponse(responseCode = "200", description = "Lấy dữ liệu thành công")
    @ApiResponse(responseCode = "404", description = "Người dùng không tồn tại")
    public ResponseEntity<List<SmokingHistoryResponse>> getUserSmokingHistory(
            @Parameter(description = "ID người dùng") @PathVariable Integer userId) {
        return smokingHistoryService.getUserSmokingHistory(userId);
    }

    @GetMapping("/user/{userId}/latest")
    @Operation(summary = "Lấy lịch sử hút thuốc mới nhất", description = "Lấy thông tin lịch sử hút thuốc gần nhất của người dùng")
    @ApiResponse(responseCode = "200", description = "Lấy dữ liệu thành công")
    @ApiResponse(responseCode = "404", description = "Không tìm thấy dữ liệu")
    public ResponseEntity<SmokingHistoryResponse> getLatestSmokingHistory(
            @Parameter(description = "ID người dùng") @PathVariable Integer userId) {
        return smokingHistoryService.getLatestSmokingHistory(userId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết lịch sử hút thuốc", description = "Lấy thông tin chi tiết của một record lịch sử hút thuốc")
    @ApiResponse(responseCode = "200", description = "Lấy dữ liệu thành công")
    @ApiResponse(responseCode = "404", description = "Không tìm thấy")
    public ResponseEntity<SmokingHistoryResponse> getSmokingHistoryById(
            @Parameter(description = "ID lịch sử hút thuốc") @PathVariable Integer id) {
        return smokingHistoryService.getSmokingHistoryById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật lịch sử hút thuốc", description = "Cập nhật thông tin lịch sử hút thuốc")
    @ApiResponse(responseCode = "200", description = "Cập nhật thành công")
    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    @ApiResponse(responseCode = "404", description = "Không tìm thấy")
    public ResponseEntity<?> updateSmokingHistory(
            @Parameter(description = "ID lịch sử hút thuốc") @PathVariable Integer id,
            @Valid @RequestBody SmokingHistoryRequest request) {
        return smokingHistoryService.updateSmokingHistory(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa lịch sử hút thuốc", description = "Xóa một record lịch sử hút thuốc")
    @ApiResponse(responseCode = "200", description = "Xóa thành công")
    @ApiResponse(responseCode = "404", description = "Không tìm thấy")
    public ResponseEntity<?> deleteSmokingHistory(
            @Parameter(description = "ID lịch sử hút thuốc") @PathVariable Integer id) {
        return smokingHistoryService.deleteSmokingHistory(id);
    }

    @GetMapping("/user/{userId}/statistics")
    @Operation(summary = "Thống kê lịch sử hút thuốc", description = "Lấy các thống kê chi tiết về thói quen hút thuốc")
    @ApiResponse(responseCode = "200", description = "Lấy thống kê thành công")
    public ResponseEntity<?> getSmokingStatistics(
            @Parameter(description = "ID người dùng") @PathVariable Integer userId) {
        return smokingHistoryService.getSmokingStatistics(userId);
    }
}
