package uz.nextqadam.bot.admin;

import java.util.List;

import uz.nextqadam.bot.common.errorlog.ErrorLogEntity;
import uz.nextqadam.bot.user.UserRepository.UserSummaryProjection;

public interface AdminService {

    SystemStats getSystemStats();

    /**
     * Barcha foydalanuvchiga xabar yuboradi. Har so'rov orasida qisqa kutish bor — Telegram
     * flood-limitiga tegib qolmaslik uchun. Bot bloklagan foydalanuvchilar failedCount'ga qo'shiladi.
     */
    BroadcastResult broadcastMessage(String message);

    /**
     * Ism yoki Telegram ID bo'yicha qidiruv, natija 10 tagacha cheklanadi.
     */
    List<UserSummaryProjection> searchUsers(String query);

    List<ErrorLogEntity> getRecentErrors(int limit);

    record BroadcastResult(int totalRecipients, int sentCount, int failedCount) {
    }
}
