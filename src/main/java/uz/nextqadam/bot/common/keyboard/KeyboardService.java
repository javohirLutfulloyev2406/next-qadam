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
     * /memory ro'yxatida bitta tugma uchun kerakli minimal ma'lumot — KeyboardService'ni memory modulining
     * MemoryItem entity'siga bog'lab qo'ymaslik uchun shu yerda alohida (yupqa) DTO sifatida e'lon qilingan.
     */
    record MemoryListOption(UUID id, String label) {
    }
}