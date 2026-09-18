package uz.nextqadam.bot.common.telegram;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import jakarta.annotation.PostConstruct;

@Component
public class TelegramBotInitializer {

    private final TelegramBotFacade telegramBotFacade;

    public TelegramBotInitializer(TelegramBotFacade telegramBotFacade) {
        this.telegramBotFacade = telegramBotFacade;
    }

    @PostConstruct
    public void registerBot() {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(telegramBotFacade);
        } catch (TelegramApiException e) {
            throw new IllegalStateException("Telegram botni ro'yxatdan o'tkazishda xatolik yuz berdi", e);
        }
    }
}
