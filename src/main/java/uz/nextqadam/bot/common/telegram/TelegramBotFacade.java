package uz.nextqadam.bot.common.telegram;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class TelegramBotFacade extends TelegramLongPollingBot {

    private final TelegramBotProperties properties;
    private final UpdateDispatcher updateDispatcher;

    public TelegramBotFacade(TelegramBotProperties properties, UpdateDispatcher updateDispatcher) {
        this.properties = properties;
        this.updateDispatcher = updateDispatcher;
    }

    @Override
    public String getBotUsername() {
        return properties.getUsername();
    }

    @Override
    public String getBotToken() {
        return properties.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        updateDispatcher.dispatch(update);
    }
}