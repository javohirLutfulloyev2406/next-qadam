package uz.nextqadam.bot.memory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoryItemRepository extends JpaRepository<MemoryItem, UUID> {

    Optional<MemoryItem> findByUserIdAndKey(UUID userId, String key);

    List<MemoryItem> findAllByUserId(UUID userId);
}