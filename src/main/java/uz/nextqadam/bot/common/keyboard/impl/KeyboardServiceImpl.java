package uz.nextqadam.bot.common.keyboard.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import uz.nextqadam.bot.common.keyboard.KeyboardService;

@Service
public class KeyboardServiceImpl implements KeyboardService {

    @Override
    public InlineKeyboardMarkup createInlineKeyboard(List<String> labels, List<String> callbackData, int columns) {
        // TODO: implementatsiya
        throw new UnsupportedOperationException("Not implemented yet");
    }
}