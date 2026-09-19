package uz.nextqadam.bot.track;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CheckInRepository extends JpaRepository<CheckIn, UUID> {

    Optional<CheckIn> findByUserIdAndDateAndType(UUID userId, LocalDate date, CheckIn.Type type);

    List<CheckIn> findAllByUserIdAndDateBetween(UUID userId, LocalDate from, LocalDate to);

    /**
     * Admin statistikasidagi "bugun faol" hisobiga qo'shiladigan — bugun kamida bitta CheckIn
     * yozgan foydalanuvchilarning distinct ID ro'yxati.
     */
    @Query("SELECT DISTINCT c.user.id FROM CheckIn c WHERE c.date = :date")
    List<UUID> findDistinctUserIdsByDate(LocalDate date);

    /**
     * ResetService.softReset uchun bulk soft-delete.
     */
    @Modifying
    @Query("UPDATE CheckIn c SET c.deleted = true WHERE c.user.id = :userId")
    void softDeleteAllByUserId(@Param("userId") UUID userId);

    /**
     * ResetService.hardDelete uchun — native SQL (sabab: GoalRepository.hardDeleteAllByUserId
     * javdoc'iga qarang).
     */
    @Modifying
    @Query(value = "DELETE FROM check_ins WHERE user_id = :userId", nativeQuery = true)
    void hardDeleteAllByUserId(@Param("userId") UUID userId);
}
