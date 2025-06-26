package edu.fpt.smokingcessionnew.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Size(max = 255)
    @Nationalized
    @Column(name = "quit_method")
    private String quitMethod;

    @Column(name = "daily_reduction_target")
    private Integer dailyReductionTarget;

    @Nationalized
    @Column(name = "milestone_goals", columnDefinition = "NVARCHAR(MAX)")
    private String milestoneGoals;

    @Nationalized
    @Column(name = "personalized_strategies", columnDefinition = "NVARCHAR(MAX)")
    private String personalizedStrategies;

    @Column(name = "created_date", columnDefinition = "datetime")
    private LocalDateTime createdDate;

    @Column(name = "status")
    private Integer status;

    @PrePersist
    protected void onCreate() {
        this.createdDate = LocalDateTime.now();
    }
}