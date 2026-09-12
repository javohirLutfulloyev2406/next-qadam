package uz.nextqadam.bot.common.errorlog;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ErrorLogRepository extends JpaRepository<ErrorLogEntity, UUID> {
}