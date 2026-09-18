package uz.nextqadam.bot.memory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemoryService {

    /**
     * (userId, key) bo'yicha upsert: mavjud bo'lsa value/importance (va updatedAt) yangilanadi,
     * aks holda yangi MemoryItem yaratiladi.
     */
    MemoryItem remember(UUID userId, String key, String value, Integer importance);

    Optional<MemoryItem> recall(UUID userId, String key);

    List<MemoryItem> recallAll(UUID userId);

    /**
     * importance bo'yicha kamayish tartibida saralangan eng muhim {limit} ta MemoryItem.
     */
    List<MemoryItem> getTopMemories(UUID userId, int limit);

    /**
     * Eng muhim 5 ta MemoryItem'ni "- {key}: {value}" formatida qatorlarga tizib, LLM promptiga
     * qo'shish uchun tayyor matn qaytaradi. Hech narsa topilmasa bo'sh string qaytaradi.
     */
    String buildContextBlock(UUID userId);

    void forget(UUID userId, String key);

    void forgetOne(UUID userId, UUID memoryItemId);

    void forgetAll(UUID userId);
}
