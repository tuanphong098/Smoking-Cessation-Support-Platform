package edu.fpt.smokingcessionnew.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
public class SmokingHistory {
    @Id
    @Column(name = "smoking_history_id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "cigarettes_per_day")
    private Integer cigarettesPerDay;

    @Column(name = "price_per_pack", precision = 18, scale = 2)
    private BigDecimal pricePerPack;

    @Column(name = "duration_years")
    private Integer durationYears;

    @Column(name = "age_started_smoking")
    private Integer ageStartedSmoking;

    @Size(max = 255)
    @Nationalized
    @Column(name = "brand_preference")
    private String brandPreference;

    @Nationalized
    @Column(name = "triggers", columnDefinition = "NVARCHAR(MAX)")
    private String triggers;

    @Column(name = "previous_quit_attempts")
    private Integer previousQuitAttempts;

    @Column(name = "longest_quit_period_days")
    private Integer longestQuitPeriodDays;

    @Column(name = "motivation_level")
    private Integer motivationLevel;

    @Nationalized
    @Column(name = "health_conditions_related", columnDefinition = "NVARCHAR(MAX)")
    private String healthConditionsRelated;

    @Column(name = "recorded_date")
    private LocalDate recordedDate;

    @Column(name = "updated_date")
    private LocalDate updatedDate;

}