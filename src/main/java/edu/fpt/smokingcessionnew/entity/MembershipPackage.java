package edu.fpt.smokingcessionnew.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
public class MembershipPackage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "package_id", nullable = false)
    private Integer id;

    @Column(name = "package_name", nullable = false)
    private String packageName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", precision = 18, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(name = "features", columnDefinition = "TEXT")
    private String features;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_date", columnDefinition = "datetime")
    private LocalDateTime createdDate;

    @Column(name = "updated_date", columnDefinition = "datetime")
    private LocalDateTime updatedDate;
}

