package uz.nextqadam.bot.goal;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MilestoneRepository extends JpaRepository<Milestone, UUID> {

    List<Milestone> findAllByGoalId(UUID goalId);

    /**
     * ResetService.softReset uchun bulk soft-delete.
     */
    @Modifying
    @Query("UPDATE Milestone m SET m.deleted = true WHERE m.goal.user.id = :userId")
    void softDeleteAllByUserId(@Param("userId") UUID userId);

    /**
     * ResetService.hardDelete uchun — native SQL (sabab: GoalRepository.hardDeleteAllByUserId
     * javdoc'iga qarang). Goal'dan OLDIN chaqirilishi SHART (FK: milestones.goal_id).
     */
    @Modifying
    @Query(value = "DELETE FROM milestones WHERE goal_id IN (SELECT id FROM goals WHERE user_id = :userId)",
            nativeQuery = true)
    void hardDeleteAllByUserId(@Param("userId") UUID userId);
}
