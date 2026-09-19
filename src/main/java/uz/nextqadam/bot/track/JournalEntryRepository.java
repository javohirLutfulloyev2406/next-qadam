package uz.nextqadam.bot.track;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {

    List<JournalEntry> findAllByUserId(UUID userId);

    /**
     * ResetService.softReset uchun bulk soft-delete.
     */
    @Modifying
    @Query("UPDATE JournalEntry j SET j.deleted = true WHERE j.user.id = :userId")
    void softDeleteAllByUserId(@Param("userId") UUID userId);

    /**
     * ResetService.hardDelete uchun — native SQL (sabab: GoalRepository.hardDeleteAllByUserId
     * javdoc'iga qarang).
     */
    @Modifying
    @Query(value = "DELETE FROM journal_entries WHERE user_id = :userId", nativeQuery = true)
    void hardDeleteAllByUserId(@Param("userId") UUID userId);
}
