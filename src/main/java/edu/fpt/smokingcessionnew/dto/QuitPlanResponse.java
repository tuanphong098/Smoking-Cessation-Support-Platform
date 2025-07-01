package edu.fpt.smokingcessionnew.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class QuitPlanResponse {

    private Integer id;
    private Integer userId;
    private String userFullName;
    private Integer smokingHistoryId;
    private Integer coachId;
    private String coachName;
    private LocalDate targetQuitDate;
    private String quitMethod;
    private Integer dailyReductionTarget;
    private String milestoneGoals;
    private String personalizedStrategies;
    private LocalDateTime createdDate;
    private String status; // "DRAFT", "PENDING_COACH_REVIEW", "COACH_CUSTOMIZED", "ACTIVE", "COMPLETED", "PAUSED"
    private String statusDescription;

    // Coach customization info
    private String coachNotes;
    private LocalDateTime lastModifiedByCoach;
    private Boolean isCustomizedByCoach;

    // Progress tracking
    private Integer progressPercentage;
    private Integer daysActive;
    private String nextMilestone;
}
