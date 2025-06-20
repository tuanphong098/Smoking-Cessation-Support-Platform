package edu.fpt.smokingcessionnew.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

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

    @Column(name = "title")
    private String title;

    @Lob
    @Column(name = "content")
    private String content;

    @Column(name = "notification_type")
    private Integer notificationType;

    @Column(name = "is_read")
    private Boolean isRead;

    @Column(name = "scheduled_time")
    private Instant scheduledTime;

    @Column(name = "sent_time")
    private Instant sentTime;

    @Column(name = "created_date")
    private Instant createdDate;

}