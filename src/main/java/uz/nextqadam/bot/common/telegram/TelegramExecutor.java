package uz.nextqadam.bot.common.telegram;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

public interface TelegramExecutor {

    void sendMessage(Long chatId, String text);

    /**
     * AI javobini kutish paytida ko'rsatiladigan vaqtinchalik placeholder xabarini yuboradi va
     * uning message_id'sini qaytaradi — natija tayyor bo'lgach shu ID orqali editMessageText
     * chaqirilib, placeholder haqiqiy javobga "aylantiriladi". Xatolik yuz bersa (masalan
     * foydalanuvchi botni bloklagan), {@code null} qaytaradi — chaqiruvchi bu holda oddiy
     * sendMessage'ga qaytishi kerak.
     */
    Integer sendPlaceholder(Long chatId, String text);

    /**
     * sendMessage'dan farqli o'laroq, TelegramApiException'ni yutib qo'ymaydi — muvaffaqiyat/
     * muvaffaqiyatsizlikni boolean orqali qaytaradi. Admin broadcast'ida botni bloklagan
     * foydalanuvchilarni sanash uchun kerak.
     */
    boolean sendMessageForBroadcast(Long chatId, String text);

    void sendMessageWithKeyboard(Long chatId, String text, InlineKeyboardMarkup keyboard);

    void sendMessageWithReplyKeyboard(Long chatId, String text, ReplyKeyboardMarkup keyboard);

    void editMessageText(Long chatId, Integer messageId, String newText);

    void editMessageReplyMarkup(Long chatId, Integer messageId, InlineKeyboardMarkup keyboard);

    /**
     * Callback query'ga tezkor "toast" javob (masalan cheklovni bildirish uchun). showAlert=true
     * bo'lsa, Telegram klientida modal ogohlantirish sifatida ko'rsatiladi.
     */
    void answerCallbackQuery(String callbackQueryId, String text, boolean showAlert);

    /**
     * "Yozmoqda..." kabi chat action indikatorini yuboradi (masalan AI javobini kutish paytida).
     * action qiymati Telegram Bot API'dagi ChatAction nomlariga mos bo'lishi kerak (masalan "typing").
     */
    void sendChatAction(Long chatId, String action);
}