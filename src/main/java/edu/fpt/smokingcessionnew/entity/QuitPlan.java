package edu.fpt.smokingcessionnew.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
public class QuitPlan {
    @Id
    @Column(name = "plan_id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "smoking_history_id")
    private SmokingHistory smokingHistory;

    @Column(name = "target_quit_date")
    private LocalDate targetQuitDate;

    @Column(name = "quit_method")
    private String quitMethod;

    @Column(name = "daily_reduction_target")
    private Integer dailyReductionTarget;

    @Lob
    @Column(name = "milestone_goals")
    private String milestoneGoals;

    @Lob
    @Column(name = "personalized_strategies")
    private String personalizedStrategies;

    @Column(name = "created_date")
    private Instant createdDate;

    @Column(name = "status")
    private Integer status;

}