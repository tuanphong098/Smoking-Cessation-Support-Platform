package edu.fpt.smokingcessionnew.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

@Getter
@Setter
@Entity
public class Achievement {
    @Id
    @Column(name = "achievement_id", nullable = false)
    private Integer id;

    @Size(max = 255)
    @Nationalized
    @Column(name = "achievement_name")
    private String achievementName;

    @Nationalized
    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Size(max = 255)
    @Column(name = "icon_url")
    private String iconUrl;

    @Nationalized
    @Column(name = "criteria", columnDefinition = "NVARCHAR(MAX)")
    private String criteria;

    @Column(name = "points_reward")
    private Integer pointsReward;

    @Column(name = "is_active")
    private Boolean isActive;

}