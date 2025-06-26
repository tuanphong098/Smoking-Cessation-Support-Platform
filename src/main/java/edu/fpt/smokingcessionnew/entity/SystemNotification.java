package edu.fpt.smokingcessionnew.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
public class SystemNotification {
    @Id
    @Column(name = "notification_id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Size(max = 255)
    @Column(name = "title")
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "notification_type")
    private Integer notificationType;

    @Column(name = "is_read")
    private Boolean isRead;

    @Column(name = "scheduled_time", columnDefinition = "datetime")
    private LocalDateTime scheduledTime;

    @Column(name = "sent_time", columnDefinition = "datetime")
    private LocalDateTime sentTime;

    @Column(name = "created_date", columnDefinition = "datetime")
    private LocalDateTime createdDate;
}