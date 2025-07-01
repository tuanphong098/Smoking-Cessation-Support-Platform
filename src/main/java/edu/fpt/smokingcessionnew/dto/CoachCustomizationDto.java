package edu.fpt.smokingcessionnew.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CoachCustomizationDto {

    @NotNull
    private Integer quitPlanId;

    @NotNull
    private Integer coachId;

    // Coach's customized plan details
    private LocalDate revisedTargetQuitDate;
    private String recommendedQuitMethod;
    private Integer recommendedDailyReduction;
    private String customizedMilestoneGoals;
    private String personalizedStrategies;
    private String coachNotes;
    private String coachRecommendations;

    // Coach assessment
    private String riskAssessment; // "LOW", "MEDIUM", "HIGH"
    private String difficultyLevel; // "EASY", "MODERATE", "CHALLENGING", "VERY_DIFFICULT"
    private Integer estimatedSuccessRate; // 0-100%

    // Follow-up plan
    private Integer recommendedCheckInFrequency; // days
    private String supportLevel; // "MINIMAL", "REGULAR", "INTENSIVE"
}
