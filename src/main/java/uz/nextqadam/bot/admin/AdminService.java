package uz.nextqadam.bot.admin;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import uz.nextqadam.bot.common.errorlog.ErrorLogEntity;

public interface AdminService {

    SystemStats getSystemStats();

    /**
     * Barcha foydalanuvchiga xabar yuboradi. Har so'rov orasida qisqa kutish bor — Telegram
     * flood-limitiga tegib qolmaslik uchun. Bot bloklagan foydalanuvchilar failedCount'ga qo'shiladi.
     */
    BroadcastResult broadcastMessage(String message);

    /**
     * Ism yoki Telegram ID bo'yicha qidiruv, natija 10 tagacha cheklanadi. Har bir natija bilan
     * birga so'nggi faollik ham (qo'shimcha so'rovsiz) qaytariladi.
     */
    List<UserSearchResult> searchUsers(String query);

    List<ErrorLogEntity> getRecentErrors(int limit);

    /**
     * Bitta foydalanuvchining so'nggi 30 ta harakati, eng yangisi birinchi — admin "to'liq
     * faoliyatni ko'rish" tugmasi orqali.
     */
    List<UserActivityLog> getUserActivity(UUID userId);

    Optional<LastActivityInfo> getLastActivity(UUID userId);

    record BroadcastResult(int totalRecipients, int sentCount, int failedCount) {
    }

    record LastActivityInfo(Instant occurredAt, String actionDetail) {
    }

    record UserSearchResult(UUID id, String name, Long telegramId, Optional<LastActivityInfo> lastActivity) {
    }
}
