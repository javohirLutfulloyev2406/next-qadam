package uz.nextqadam.bot.goal;

import java.time.Instant;

import uz.nextqadam.bot.common.BaseEntity;

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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "tasks",
        indexes = @Index(name = "idx_task_status_due_date", columnList = "status, due_date"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Task extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_id", nullable = true)
    private Milestone milestone;

    @Column(name = "title")
    private String title;

    @Column(name = "due_date")
    private Instant dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Builder.Default
    @Column(name = "is_today_priority", nullable = false)
    private boolean isTodayPriority = false;

    @Builder.Default
    @Column(name = "consecutive_snooze_count", nullable = false)
    private int consecutiveSnoozeCount = 0;

    @Column(name = "snoozed_until")
    private Instant snoozedUntil;

    public enum Status {
        PENDING,
        DONE,
        SNOOZED
    }
}
