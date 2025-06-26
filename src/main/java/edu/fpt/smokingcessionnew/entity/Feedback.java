package edu.fpt.smokingcessionnew.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
public class Feedback {
    @Id
    @Column(name = "feedback_id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private QuitPlan plan;

    @Column(name = "target_type")
    private Integer targetType;

    @Column(name = "target_id")
    private Integer targetId;

    @Column(name = "rating")
    private Integer rating;

    @Nationalized
    @Column(name = "comment", columnDefinition = "NVARCHAR(MAX)")
    private String comment;

    @Column(name = "created_date", columnDefinition = "datetime")
    private LocalDateTime createdDate;

    @Column(name = "is_reviewed")
    private Boolean isReviewed;

}