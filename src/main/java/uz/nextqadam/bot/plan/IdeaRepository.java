package uz.nextqadam.bot.plan;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdeaRepository extends JpaRepository<Idea, UUID> {

    List<Idea> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    /**
     * ResetService.softReset uchun bulk soft-delete.
     */
    @Modifying
    @Query("UPDATE Idea i SET i.deleted = true WHERE i.user.id = :userId")
    void softDeleteAllByUserId(@Param("userId") UUID userId);

    /**
     * ResetService.hardDelete uchun — native SQL (sabab: GoalRepository.hardDeleteAllByUserId
     * javdoc'iga qarang).
     */
    @Modifying
    @Query(value = "DELETE FROM ideas WHERE user_id = :userId", nativeQuery = true)
    void hardDeleteAllByUserId(@Param("userId") UUID userId);
}
