package uz.nextqadam.bot.common.keyboard;

import java.util.List;
import java.util.UUID;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

public interface KeyboardService {

    InlineKeyboardMarkup createInlineKeyboard(List<String> labels, List<String> callbackData, int columns);

    ReplyKeyboardMarkup buildMainMenuKeyboard();

    /**
     * Ton tanlash tugmalarini (Yumshoq/Oddiy/Qattiq/Hardcore) berilgan callbackData prefiksi bilan quradi —
     * onboarding va profil ("uslubni o'zgartirish") oqimlari bir xil tugmalarni farqli prefiks bilan qayta
     * ishlatishi uchun umumlashtirilgan.
     */
    InlineKeyboardMarkup buildToneSelectionKeyboard(String callbackPrefix);

    InlineKeyboardMarkup buildProfileMenuKeyboard();

    InlineKeyboardMarkup buildMemoryListKeyboard(List<MemoryListOption> items);

    InlineKeyboardMarkup buildConfirmDeleteAllKeyboard();

    /**
     * Bitta Task uchun "✅ Bajardim" tugmasi (callbackData "TASK_DONE_{taskId}") — /nextstep va /goals
     * oqimlarida bir xil interaktiv kartochka ko'rinishini ta'minlaydi.
     */
    InlineKeyboardMarkup buildTaskActionKeyboard(UUID taskId);

    /**
     * /goals panelidagi har bir Goal bloki uchun "📌 Keyingi qadamni ko'rish" tugmasi
     * (callbackData "GOALS_NEXTSTEP_{goalId}").
     */
    InlineKeyboardMarkup buildGoalsNextStepKeyboard(UUID goalId);

    /**
     * Goal Drift Detection ogohlantirishidagi 2 tugma: "🔄 Maqsadni yangilash" (DRIFT_UPDATE_GOAL) va
     * "Yo'q, davom etaman" (DRIFT_DISMISS).
     */
    InlineKeyboardMarkup buildGoalDriftKeyboard();

    /**
     * /help va onboarding oxiridagi to'liq qo'llanma havolasi — callbackData yoki oddiy url emas, Web App
     * turidagi tugma (bosilganda sahifa Telegram ichida, native ko'rinishda ochiladi — tashqi brauzerga
     * chiqmaydi).
     */
    InlineKeyboardMarkup buildGuideLinkKeyboard();

    /**
     * /admin panelining asosiy menyusi — faqat AdminHandler orqali, isAdmin tekshiruvidan o'tgan
     * chaqiruvchilarga ko'rsatiladi.
     */
    InlineKeyboardMarkup buildAdminMenuKeyboard();

    /**
     * Admin broadcast oqimidagi ikkinchi bosqich — "✅ Ha, yubor" (ADMIN_BROADCAST_CONFIRM) va
     * "❌ Bekor qilish" (ADMIN_BROADCAST_CANCEL) tugmalari.
     */
    InlineKeyboardMarkup buildAdminBroadcastConfirmKeyboard();

    /**
     * /reset boshlang'ich tanlovi — "🔄 Yangidan boshlash" (RESET_SOFT_ASK) va "🗑 Butunlay
     * o'chirish" (RESET_HARD_ASK), har biri alohida qatorda.
     */
    InlineKeyboardMarkup buildResetChoiceKeyboard();

    /**
     * "Yangidan boshlash" oqimining yakuniy tasdiqlash bosqichi — "✅ Ha, boshlaymiz"
     * (RESET_SOFT_CONFIRM) va "❌ Bekor qilish" (RESET_CANCEL).
     */
    InlineKeyboardMarkup buildResetSoftConfirmKeyboard();

    /**
     * "Butunlay o'chirish" oqimining yakuniy tasdiqlash bosqichi — "✅ Ha, butunlay o'chir"
     * (RESET_HARD_CONFIRM) va "❌ Bekor qilish" (RESET_CANCEL).
     */
    InlineKeyboardMarkup buildResetHardConfirmKeyboard();

    /**
     * Ertalabki check-in (/planday) uchun ko'p tanlovli klaviatura — har bir task uchun checkbox
     * tugmasi (callbackData "CHECKIN_TOGGLE_{taskId}") va oxirida "✅ Tasdiqlash (N/3)" tugmasi
     * (callbackData "CHECKIN_CONFIRM").
     */
    InlineKeyboardMarkup buildMorningCheckinKeyboard(List<CheckinTaskOption> options);

    /**
     * /memory ro'yxatida bitta tugma uchun kerakli minimal ma'lumot — KeyboardService'ni memory modulining
     * MemoryItem entity'siga bog'lab qo'ymaslik uchun shu yerda alohida (yupqa) DTO sifatida e'lon qilingan.
     */
    record MemoryListOption(UUID id, String label) {
    }

    /**
     * buildMorningCheckinKeyboard uchun kerakli minimal ma'lumot — KeyboardService'ni goal modulining
     * Task entity'siga bog'lab qo'ymaslik uchun shu yerda alohida (yupqa) DTO sifatida e'lon qilingan.
     */
    record CheckinTaskOption(UUID id, String title, boolean selected) {
    }
}