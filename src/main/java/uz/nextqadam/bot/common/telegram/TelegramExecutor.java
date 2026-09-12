package uz.nextqadam.bot.common.telegram;

public interface TelegramExecutor {

    void sendMessage(Long chatId, String text);

    void sendMessageWithKeyboard(Long chatId, String text, Object keyboard);

    void editMessage(Long chatId, Integer messageId, String newText);
}