package uz.nextqadam.bot.companion;

import java.util.UUID;

public interface CompanionService {

    String respondToMessage(UUID userId, String message);

    String motivate(UUID userId);

    String handleSos(UUID userId);
}
