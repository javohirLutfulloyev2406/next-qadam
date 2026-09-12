package uz.nextqadam.bot.memory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemoryService {

    MemoryItem remember(UUID userId, String key, String value, Integer importance);

    Optional<MemoryItem> recall(UUID userId, String key);

    List<MemoryItem> recallAll(UUID userId);

    void forget(UUID userId, String key);

    void forgetAll(UUID userId);
}
