package uz.nextqadam.bot.common.telegram;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

public interface TelegramExecutor {

    void sendMessage(Long chatId, String text);

    void sendMessageWithKeyboard(Long chatId, String text, InlineKeyboardMarkup keyboard);

    void sendMessageWithReplyKeyboard(Long chatId, String text, ReplyKeyboardMarkup keyboard);

    void editMessage(Long chatId, Integer messageId, String newText);
}