package uz.nextqadam.bot.nudge;

import java.time.Instant;

import org.hibernate.annotations.SQLRestriction;

import uz.nextqadam.bot.common.BaseEntity;
import uz.nextqadam.bot.common.enums.ToneType;
import uz.nextqadam.bot.goal.Task;
import uz.nextqadam.bot.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "reminders",
        indexes = @Index(name = "idx_reminder_status_scheduled_at", columnList = "status, scheduled_at"))
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Reminder extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = true)
    private Task task;

    // task=null bo'lgan eslatmalar uchun (masalan Brain Dump'dan kelgan) — eslatma matnini saqlash uchun kerak,
    // aks holda ReminderScheduler yuborish vaqti kelganda NIMA haqida eslatish kerakligini bilmay qoladi.
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "tone")
    private ToneType tone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    public enum Status {
        PENDING,
        SENT,
        SNOOZED,
        CANCELLED
    }
}
