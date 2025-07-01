package edu.fpt.smokingcessionnew.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class QuitPlanRequest {

    private Integer smokingHistoryId;
    private Integer coachId; // Optional - nếu user chọn coach cụ thể
    private LocalDate targetQuitDate;
    private String quitMethod; // "gradual", "cold_turkey", "substitution"
    private Integer dailyReductionTarget;
    private String personalGoals;
    private String preferredStrategies;
    private String additionalNotes;
}
