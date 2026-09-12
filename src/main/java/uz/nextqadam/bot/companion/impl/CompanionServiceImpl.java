package uz.nextqadam.bot.companion.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;

import uz.nextqadam.bot.companion.CompanionService;

@Service
public class CompanionServiceImpl implements CompanionService {

    @Override
    public String respondToMessage(UUID userId, String message) {
        // TODO: implementatsiya
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public String motivate(UUID userId) {
        // TODO: implementatsiya
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public String handleSos(UUID userId) {
        // TODO: implementatsiya
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
