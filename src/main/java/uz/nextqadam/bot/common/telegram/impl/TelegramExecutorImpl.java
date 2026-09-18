package uz.nextqadam.bot.common.telegram.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import uz.nextqadam.bot.common.telegram.TelegramBotFacade;
import uz.nextqadam.bot.common.telegram.TelegramExecutor;

@Component
public class TelegramExecutorImpl implements TelegramExecutor {

    private static final Logger log = LoggerFactory.getLogger(TelegramExecutorImpl.class);

    private final TelegramBotFacade telegramBotFacade;

    public TelegramExecutorImpl(@Lazy TelegramBotFacade telegramBotFacade) {
        this.telegramBotFacade = telegramBotFacade;
    }

    @Override
    public void sendMessage(Long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(text)
                .build();
        try {
            telegramBotFacade.execute(message);
        } catch (TelegramApiException e) {
            log.error("Telegram xabar yuborishda xatolik: chatId={}", chatId, e);
        }
    }

    @Override
    public void sendMessageWithKeyboard(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage message = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(text)
                .replyMarkup(keyboard)
                .build();
        try {
            telegramBotFacade.execute(message);
        } catch (TelegramApiException e) {
            log.error("Telegram xabar (klaviatura bilan) yuborishda xatolik: chatId={}", chatId, e);
        }
    }

    @Override
    public void sendMessageWithReplyKeyboard(Long chatId, String text, ReplyKeyboardMarkup keyboard) {
        SendMessage message = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(text)
                .replyMarkup(keyboard)
                .build();
        try {
            telegramBotFacade.execute(message);
        } catch (TelegramApiException e) {
            log.error("Telegram xabar (reply klaviatura bilan) yuborishda xatolik: chatId={}", chatId, e);
        }
    }

    @Override
    public void editMessage(Long chatId, Integer messageId, String newText) {
        EditMessageText message = EditMessageText.builder()
                .chatId(String.valueOf(chatId))
                .messageId(messageId)
                .text(newText)
                .build();
        try {
            telegramBotFacade.execute(message);
        } catch (TelegramApiException e) {
            log.error("Telegram xabarni tahrirlashda xatolik: chatId={}, messageId={}", chatId, messageId, e);
        }
    }
}
