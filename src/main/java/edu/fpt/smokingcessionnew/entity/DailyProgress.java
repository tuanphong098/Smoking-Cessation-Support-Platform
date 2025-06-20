package edu.fpt.smokingcessionnew.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
public class DailyProgress {
    @Id
    @Column(name = "progress_id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private QuitPlan plan;

    @Column(name = "\"date\"")
    private LocalDate date;

    @Column(name = "cigarettes_smoked")
    private Integer cigarettesSmoked;

    @Column(name = "money_saved", precision = 18, scale = 2)
    private BigDecimal moneySaved;

    @Column(name = "health_score")
    private Integer healthScore;

    @Column(name = "mood_level")
    private Integer moodLevel;

    @Column(name = "craving_intensity")
    private Integer cravingIntensity;

    @Lob
    @Column(name = "notes")
    private String notes;

    @Column(name = "created_date")
    private Instant createdDate;

}