package uz.nextqadam.bot.memory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemoryItemRepository extends JpaRepository<MemoryItem, UUID> {

    Optional<MemoryItem> findByUserIdAndKey(UUID userId, String key);

    List<MemoryItem> findAllByUserId(UUID userId);

    List<MemoryItem> findByUserIdOrderByImportanceDesc(UUID userId, Pageable pageable);

    Optional<MemoryItem> findByIdAndUserId(UUID id, UUID userId);

    void deleteAllByUserId(UUID userId);

    /**
     * ResetService.softReset uchun bulk soft-delete — /forget oqimidagi deleteAllByUserId'dan
     * farqli o'laroq, qatorni haqiqatan o'chirmaydi, faqat yashiradi.
     */
    @Modifying
    @Query("UPDATE MemoryItem m SET m.deleted = true WHERE m.user.id = :userId")
    void softDeleteAllByUserId(@Param("userId") UUID userId);

    /**
     * ResetService.hardDelete uchun — native SQL (sabab: GoalRepository.hardDeleteAllByUserId
     * javdoc'iga qarang) — allaqachon softReset orqali soft-delete qilingan qatorlarni ham
     * qamrab oladi, oddiy deleteAllByUserId esa @SQLRestriction tufayli ularni ko'rmaydi.
     */
    @Modifying
    @Query(value = "DELETE FROM memory_items WHERE user_id = :userId", nativeQuery = true)
    void hardDeleteAllByUserId(@Param("userId") UUID userId);
}