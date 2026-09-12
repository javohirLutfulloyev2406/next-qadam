package uz.nextqadam.bot.common.keyboard;

import java.util.List;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

public interface KeyboardService {

    InlineKeyboardMarkup createInlineKeyboard(List<String> labels, List<String> callbackData, int columns);
}