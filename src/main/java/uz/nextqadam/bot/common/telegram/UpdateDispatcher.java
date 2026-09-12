package uz.nextqadam.bot.common.telegram;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class UpdateDispatcher {

    // TODO: logId generatsiya qilish va update turiga qarab tegishli modul Handler'iga yo'naltirish
    public void dispatch(Update update) {
    }
}