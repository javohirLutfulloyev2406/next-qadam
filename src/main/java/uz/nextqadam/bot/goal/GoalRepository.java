package uz.nextqadam.bot.goal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GoalRepository extends JpaRepository<Goal, UUID> {

    List<Goal> findAllByUserIdAndStatus(UUID userId, Goal.Status status);

    Optional<Goal> findByUserIdAndTitle(UUID userId, String title);

    /**
     * Foydalanuvchining eng so'nggi yaratilgan faol Goal'i — Goal Drift Detection uchun.
     */
    Optional<Goal> findFirstByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, Goal.Status status);

    long countByStatus(Goal.Status status);

    /**
     * ResetService.softReset uchun — bitta bulk UPDATE bilan foydalanuvchining barcha Goal'larini
     * soft-delete qiladi, har birini birma-bir yuklab o'zgartirish shart emas.
     */
    @Modifying
    @Query("UPDATE Goal g SET g.deleted = true WHERE g.user.id = :userId")
    void softDeleteAllByUserId(@Param("userId") UUID userId);

    /**
     * ResetService.hardDelete uchun — @SQLRestriction("deleted = false") oddiy HQL bulk
     * operatsiyalarni ham cheklaydi (allaqachon soft-delete qilingan qatorlar HQL orqali
     * "ko'rinmaydi"), shu sababli HAMMA qatorni (soft-delete qilinganlarini ham) haqiqatan
     * o'chirish uchun native SQL ishlatiladi — bu Hibernate'ning restriction'ini butunlay
     * chetlab o'tadi. Task/Milestone bu Goal'larga bog'liq bo'lgani uchun ular oldinroq
     * o'chirilishi SHART (FK xatosiga yo'l qo'ymaslik uchun).
     */
    @Modifying
    @Query(value = "DELETE FROM goals WHERE user_id = :userId", nativeQuery = true)
    void hardDeleteAllByUserId(@Param("userId") UUID userId);
}
