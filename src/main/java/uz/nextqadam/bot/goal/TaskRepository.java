package uz.nextqadam.bot.goal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findAllByGoalId(UUID goalId);

    List<Task> findAllByGoal_User_Id(UUID userId);

    List<Task> findAllByStatusAndDueDateBefore(Task.Status status, Instant dueDate);

    /**
     * Foydalanuvchining barcha faol Goal'lari bo'yicha PENDING Task'larini ustuvorlik (ertalabki
     * check-in orqali belgilangan is_today_priority) birinchi, so'ng eng yaqin muddat tartibida qaytaradi.
     */
    List<Task> findByGoal_User_IdAndStatusOrderByIsTodayPriorityDescDueDateAsc(UUID userId, Task.Status status);

    /**
     * findByGoal_User_Id...'ning bitta Goal doirasidagi varianti — /goals panelidagi "Keyingi qadamni
     * ko'rish" tugmasi uchun.
     */
    Optional<Task> findFirstByGoal_IdAndStatusOrderByIsTodayPriorityDescDueDateAsc(UUID goalId, Task.Status status);

    /**
     * /planday uchun tanlov ro'yxati — ustuvorlikdan qat'i nazar, eng yaqin muddatli PENDING Task'lar.
     */
    List<Task> findByGoal_User_IdAndStatusOrderByDueDateAsc(UUID userId, Task.Status status, Pageable pageable);

    /**
     * Foydalanuvchining eng so'nggi bajargan Task'i (Companion modulida motivatsion xabar uchun).
     */
    Optional<Task> findFirstByGoal_User_IdAndStatusOrderByUpdatedAtDesc(UUID userId, Task.Status status);

    long countByGoal_User_IdAndStatus(UUID userId, Task.Status status);

    /**
     * Kechki check-in uchun "bugungi" Task'lar — muddati bugunga to'g'ri kelgan YOKI ertalabki
     * check-in orqali ustuvor deb belgilangan, hali PENDING yoki allaqachon DONE bo'lganlar.
     */
    @Query("""
            SELECT t FROM Task t
            WHERE t.goal.user.id = :userId
              AND t.status IN :statuses
              AND (t.isTodayPriority = true OR (t.dueDate >= :dayStart AND t.dueDate < :dayEnd))
            """)
    List<Task> findTodaysTasksForCheckin(@Param("userId") UUID userId, @Param("statuses") List<Task.Status> statuses,
                                          @Param("dayStart") Instant dayStart, @Param("dayEnd") Instant dayEnd);

    /**
     * Haftalik retrospektiva va Goal Drift Detection uchun — muddati berilgan oraliqqa to'g'ri
     * keluvchi Task'lar (holatidan qat'i nazar).
     */
    List<Task> findByGoal_User_IdAndDueDateBetween(UUID userId, Instant from, Instant to);

    /**
     * Snooze muddati o'tgan Task'larni qayta PENDING'ga o'tkazish uchun (NudgeService.requeueDueSnoozedTasks).
     */
    List<Task> findAllByStatusAndSnoozedUntilBefore(Task.Status status, Instant snoozedUntil);
}
