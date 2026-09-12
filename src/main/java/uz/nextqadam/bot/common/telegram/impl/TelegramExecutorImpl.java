package uz.nextqadam.bot.common.telegram.impl;

import org.springframework.stereotype.Component;

import uz.nextqadam.bot.common.telegram.TelegramBotFacade;
import uz.nextqadam.bot.common.telegram.TelegramExecutor;

@Component
public class TelegramExecutorImpl implements TelegramExecutor {

    private final TelegramBotFacade telegramBotFacade;

    public TelegramExecutorImpl(TelegramBotFacade telegramBotFacade) {
        this.telegramBotFacade = telegramBotFacade;
    }

    @Override
    public void sendMessage(Long chatId, String text) {
        // TODO: implementatsiya
    }

    @Override
    public void sendMessageWithKeyboard(Long chatId, String text, Object keyboard) {
        // TODO: implementatsiya
    }

    @Override
    public void editMessage(Long chatId, Integer messageId, String newText) {
        // TODO: implementatsiya
    }
}