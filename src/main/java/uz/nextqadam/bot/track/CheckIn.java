package uz.nextqadam.bot.track;

import java.time.LocalDate;

import uz.nextqadam.bot.common.BaseEntity;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "check_ins",
        uniqueConstraints = @UniqueConstraint(name = "uq_check_in_user_date_type", columnNames = {"user_id", "date", "type"}),
        indexes = @Index(name = "idx_check_in_user_date", columnList = "user_id, date"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CheckIn extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "date")
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private Type type;

    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "parsed_summary", columnDefinition = "TEXT")
    private String parsedSummary;

    public enum Type {
        MORNING,
        EVENING
    }
}
