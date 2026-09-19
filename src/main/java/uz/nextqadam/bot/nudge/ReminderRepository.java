package uz.nextqadam.bot.nudge;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
