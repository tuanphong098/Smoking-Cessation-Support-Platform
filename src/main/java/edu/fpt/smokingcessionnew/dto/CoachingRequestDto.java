package edu.fpt.smokingcessionnew.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CoachingRequestDto {

    @NotNull
    private Integer userId;

    @NotNull
    private Integer quitPlanId;

    @NotNull
    private Integer smokingHistoryId;

    private Integer preferredCoachId; // Optional - nếu user có coach ưa thích

    private String requestMessage; // Lời nhắn từ user đến coach

    private String urgencyLevel; // "LOW", "MEDIUM", "HIGH"
}
