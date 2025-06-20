package edu.fpt.smokingcessionnew.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Achievement {
    @Id
    @Column(name = "achievement_id", nullable = false)
    private Integer id;

    @Column(name = "achievement_name")
    private String achievementName;

    @Lob
    @Column(name = "description")
    private String description;

    @Column(name = "icon_url")
    private String iconUrl;

    @Lob
    @Column(name = "criteria")
    private String criteria;

    @Column(name = "points_reward")
    private Integer pointsReward;

    @Column(name = "is_active")
    private Boolean isActive;

}