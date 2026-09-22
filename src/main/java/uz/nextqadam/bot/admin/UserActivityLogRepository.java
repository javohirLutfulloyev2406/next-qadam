package uz.nextqadam.bot.admin;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, UUID> {

    List<UserActivityLog> findTop30ByUser_IdOrderByCreatedAtDesc(UUID userId);

    Optional<UserActivityLog> findFirstByUser_IdOrderByCreatedAtDesc(UUID userId);

    /**
     * ActivityLogCleanupScheduler uchun — jadval cheksiz o'sib ketmasligi uchun eski yozuvlarni
     * bulk o'chiradi. Qaytgan son faqat log yozish uchun ishlatiladi.
     */
    @Modifying
    @Query("DELETE FROM UserActivityLog a WHERE a.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") Instant cutoff);
}
