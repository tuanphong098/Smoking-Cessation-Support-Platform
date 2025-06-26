package edu.fpt.smokingcessionnew.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "SystemSettings")
public class SystemSetting {
    @Id
    @Column(name = "setting_id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "daily_reminder_time")
    private LocalTime dailyReminderTime;

    @Column(name = "weekly_summary_day")
    private Integer weeklySummaryDay;

    @Column(name = "notification_enabled")
    private Boolean notificationEnabled;

    @Column(name = "email_enabled")
    private Boolean emailEnabled;

    @Column(name = "sms_enabled")
    private Boolean smsEnabled;

    @Column(name = "updated_date", columnDefinition = "datetime")
    private LocalDateTime updatedDate;
}