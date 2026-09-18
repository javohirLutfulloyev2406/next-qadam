package uz.nextqadam.bot.goal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
