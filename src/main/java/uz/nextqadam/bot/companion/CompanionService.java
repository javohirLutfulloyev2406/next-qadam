package uz.nextqadam.bot.companion;

import java.util.UUID;

public interface CompanionService {

    String generateFreeChatReply(UUID userId, String userMessage);

    String generateMotivation(UUID userId);

    /**
     * Joriy Task'ni 5 daqiqalik mikro-qadamga qisqartiradi. Foydalanuvchining hech qanday faol Task'i
     * bo'lmasa, {@code null} qaytaradi — bu holatni chaqiruvchi (CompanionHandler) alohida ishlov beradi.
     */
    String generateSosMicroStep(UUID userId);
}
