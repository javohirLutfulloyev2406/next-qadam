package uz.nextqadam.bot.common.errorlog;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ErrorLogRepository extends JpaRepository<ErrorLogEntity, UUID> {

    long countByCreatedAtGreaterThanEqual(Instant since);

    List<ErrorLogEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}