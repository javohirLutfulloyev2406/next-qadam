package uz.nextqadam.bot.memory.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import uz.nextqadam.bot.memory.MemoryItem;
import uz.nextqadam.bot.memory.MemoryItemRepository;
import uz.nextqadam.bot.memory.MemoryService;

@Service
public class MemoryServiceImpl implements MemoryService {

    private final MemoryItemRepository memoryItemRepository;

    public MemoryServiceImpl(MemoryItemRepository memoryItemRepository) {
        this.memoryItemRepository = memoryItemRepository;
    }

    @Override
    public MemoryItem remember(UUID userId, String key, String value, Integer importance) {
        // TODO: implementatsiya
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<MemoryItem> recall(UUID userId, String key) {
        // TODO: implementatsiya
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<MemoryItem> recallAll(UUID userId) {
        // TODO: implementatsiya
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void forget(UUID userId, String key) {
        // TODO: implementatsiya (haqiqiy hard-delete /forget uchun)
    }

    @Override
    public void forgetAll(UUID userId) {
        // TODO: implementatsiya (haqiqiy hard-delete /forget uchun)
    }
}