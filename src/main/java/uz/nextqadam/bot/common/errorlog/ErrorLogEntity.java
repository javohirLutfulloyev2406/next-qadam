package uz.nextqadam.bot.common.errorlog;

import uz.nextqadam.bot.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "error_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ErrorLogEntity extends BaseEntity {

    @Column(name = "source_module")
    private String sourceModule;

    @Column(name = "exception_type")
    private String exceptionType;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(name = "log_id")
    private String logId;

    @Builder.Default
    @Column(name = "notified", nullable = false)
    private boolean notified = false;
}