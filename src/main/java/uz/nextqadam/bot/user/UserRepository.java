package uz.nextqadam.bot.user;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import uz.nextqadam.bot.common.enums.ToneType;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByTelegramId(Long telegramId);

    long countByCreatedAtBetween(Instant from, Instant to);

    /**
     * Admin panelidagi qidiruv — ism bo'yicha LIKE (katta-kichik harfga sezgir emas) YOKI Telegram ID
     * bo'yicha qism moslik. Natija chaqiruvchi tomonda (AdminService) 10 tagacha cheklanadi.
     */
    @Query("""
            SELECT u.telegramId as telegramId, u.name as name, u.tonePreference as tonePreference,
                   u.createdAt as createdAt
            FROM User u
            WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%'))
               OR CAST(u.telegramId AS string) LIKE CONCAT('%', :query, '%')
            ORDER BY u.createdAt DESC
            """)
    List<UserSummaryProjection> searchUsers(@Param("query") String query, Pageable pageable);

    interface UserSummaryProjection {
        Long getTelegramId();

        String getName();

        ToneType getTonePreference();

        Instant getCreatedAt();
    }
}