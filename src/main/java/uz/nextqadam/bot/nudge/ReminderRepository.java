package uz.nextqadam.bot.nudge;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    /**
     * Muddati kelgan PENDING eslatmalar — ReminderScheduler foydalanuvchiga xabar yuborish uchun
     * reminder.getUser().getTelegramId() ga murojaat qiladi; User lazy assotsiatsiya bo'lgani uchun
     * (repository chaqiruvi tugagach LazyInitializationException chiqmasligi uchun) JOIN FETCH bilan
     * bitta so'rovda birga yuklaymiz.
     */
    @Query("SELECT r FROM Reminder r JOIN FETCH r.user WHERE r.status = :status AND r.scheduledAt <= :now")
    List<Reminder> findDueRemindersWithUser(@Param("status") Reminder.Status status, @Param("now") Instant now);

    /**
     * ResetService.softReset uchun bulk soft-delete.
     */
    @Modifying
    @Query("UPDATE Reminder r SET r.deleted = true WHERE r.user.id = :userId")
    void softDeleteAllByUserId(@Param("userId") UUID userId);

    /**
     * ResetService.hardDelete uchun — native SQL (sabab: GoalRepository.hardDeleteAllByUserId
     * javdoc'iga qarang). Task'dan OLDIN chaqirilishi SHART (FK: reminders.task_id).
     */
    @Modifying
    @Query(value = "DELETE FROM reminders WHERE user_id = :userId", nativeQuery = true)
    void hardDeleteAllByUserId(@Param("userId") UUID userId);
}
