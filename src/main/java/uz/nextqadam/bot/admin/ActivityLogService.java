package uz.nextqadam.bot.admin;

import java.util.UUID;

import uz.nextqadam.bot.admin.UserActivityLog.ActionType;

public interface ActivityLogService {

    /**
     * Foydalanuvchi harakatini fon vazifasi sifatida qayd etadi. HECH QACHON istisno otmaydi —
     * xatolik bo'lsa faqat log.warn yozadi, chunki bu ikkinchi darajali funksiya bo'lib, asosiy
     * bot oqimini hech qachon to'xtatmasligi kerak.
     */
    void logActivity(UUID userId, ActionType type, String detail);
}
