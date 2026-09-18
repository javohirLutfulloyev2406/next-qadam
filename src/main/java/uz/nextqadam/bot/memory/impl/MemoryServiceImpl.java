package uz.nextqadam.bot.memory.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import uz.nextqadam.bot.common.exception.NextQadamException;
import uz.nextqadam.bot.memory.MemoryItem;
import uz.nextqadam.bot.memory.MemoryItemRepository;
import uz.nextqadam.bot.memory.MemoryService;
import uz.nextqadam.bot.user.UserRepository;

@Service
public class MemoryServiceImpl implements MemoryService {

    private static final int CONTEXT_BLOCK_LIMIT = 5;

    // MemoryItem.value ustuni JSONB — Postgres faqat haqiqiy JSON matnini qabul qiladi, shu sababli
    // oddiy String qiymatlarni saqlashdan oldin JSON-qator sifatida kodlaymiz (qo'shtirnoq bilan
    // o'raymiz) va o'qishda avtomatik ravishda asl matnga qaytaramiz — chaqiruvchi bu detaldan bexabar.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final MemoryItemRepository memoryItemRepository;
    private final UserRepository userRepository;

    public MemoryServiceImpl(MemoryItemRepository memoryItemRepository, UserRepository userRepository) {
        this.memoryItemRepository = memoryItemRepository;
        this.userRepository = userRepository;
    }

    @Override
    public MemoryItem remember(UUID userId, String key, String value, Integer importance) {
        MemoryItem item = memoryItemRepository.findByUserIdAndKey(userId, key)
                .orElseGet(() -> MemoryItem.builder()
                        .user(userRepository.getReferenceById(userId))
                        .key(key)
                        .build());

        item.setValue(encodeJsonValue(value));
        item.setImportance(importance);

        MemoryItem saved = memoryItemRepository.save(item);
        saved.setValue(value);
        return saved;
    }

    @Override
    public Optional<MemoryItem> recall(UUID userId, String key) {
        return memoryItemRepository.findByUserIdAndKey(userId, key).map(this::withReadableValue);
    }

    @Override
    public List<MemoryItem> recallAll(UUID userId) {
        return memoryItemRepository.findAllByUserId(userId).stream()
                .map(this::withReadableValue)
                .toList();
    }

    @Override
    public List<MemoryItem> getTopMemories(UUID userId, int limit) {
        return memoryItemRepository.findByUserIdOrderByImportanceDesc(userId, PageRequest.of(0, limit)).stream()
                .map(this::withReadableValue)
                .toList();
    }

    @Override
    public String buildContextBlock(UUID userId) {
        List<MemoryItem> topMemories = getTopMemories(userId, CONTEXT_BLOCK_LIMIT);
        if (topMemories.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (MemoryItem item : topMemories) {
            sb.append("- ").append(item.getKey()).append(": ").append(item.getValue()).append("\n");
        }
        return sb.toString().stripTrailing();
    }

    @Override
    public void forget(UUID userId, String key) {
        memoryItemRepository.findByUserIdAndKey(userId, key).ifPresent(memoryItemRepository::delete);
    }

    @Override
    public void forgetOne(UUID userId, UUID memoryItemId) {
        memoryItemRepository.findByIdAndUserId(memoryItemId, userId).ifPresent(memoryItemRepository::delete);
    }

    @Override
    public void forgetAll(UUID userId) {
        memoryItemRepository.deleteAllByUserId(userId);
    }

    private MemoryItem withReadableValue(MemoryItem item) {
        item.setValue(decodeJsonValue(item.getValue()));
        return item;
    }

    private String encodeJsonValue(String rawValue) {
        try {
            return OBJECT_MAPPER.writeValueAsString(rawValue);
        } catch (JsonProcessingException e) {
            throw new NextQadamException("Xotira qiymatini JSON'ga o'girib bo'lmadi", e);
        }
    }

    private String decodeJsonValue(String storedValue) {
        if (storedValue == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(storedValue, String.class);
        } catch (JsonProcessingException e) {
            // Eski/format-siz qiymatlar uchun zaxira yo'l — qiymatni o'zgarishsiz qaytaramiz.
            return storedValue;
        }
    }
}
