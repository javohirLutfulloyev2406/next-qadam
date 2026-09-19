package uz.nextqadam.bot.user;

import java.util.UUID;

public interface ResetService {

    /**
     * Foydalanuvchining Goal/Milestone/Task/CheckIn/JournalEntry/Reminder/MemoryItem/Idea
     * yozuvlarini soft-delete qiladi (deleted=true) va User profilini bo'shatadi
     * (name=null, tonePreference=NORMAL, timezone=null). User qatorining o'zi — telegramId
     * bilan birga — saqlanib qoladi.
     */
    void softReset(UUID userId);

    /**
     * Foydalanuvchiga tegishli BARCHA yozuvlarni (User'ning o'zi bilan birga) bazadan
     * qaytarib bo'lmaydigan tarzda butunlay o'chiradi.
     */
    void hardDelete(UUID userId);
}
