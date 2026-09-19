package uz.nextqadam.bot.common;

public interface StateCleanupService {

    /**
     * Barcha modullardagi (Onboarding, Goal, Plan, Profile, Track) shu chatId uchun saqlangan
     * in-memory suhbat holatini (AWAITING_*) tozalaydi. ResetHandler softReset/hardDelete
     * chaqirgandan keyin — eski holat qolib ketib, foydalanuvchining keyingi xabari noto'g'ri
     * oqimga yo'naltirilmasligi uchun ishlatiladi.
     */
    void clearAll(Long chatId);
}
