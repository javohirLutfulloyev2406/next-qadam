package uz.nextqadam.bot.common.keyboard.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import uz.nextqadam.bot.common.keyboard.KeyboardService;

@Service
public class KeyboardServiceImpl implements KeyboardService {

    @Override
    public InlineKeyboardMarkup createInlineKeyboard(List<String> labels, List<String> callbackData, int columns) {
        if (labels == null || callbackData == null || labels.size() != callbackData.size()) {
            throw new IllegalArgumentException("labels va callbackData bir xil o'lchamda bo'lishi kerak");
        }
        if (columns <= 0) {
            throw new IllegalArgumentException("columns musbat son bo'lishi kerak");
        }

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> currentRow = new ArrayList<>();

        for (int i = 0; i < labels.size(); i++) {
            InlineKeyboardButton button = InlineKeyboardButton.builder()
                    .text(labels.get(i))
                    .callbackData(callbackData.get(i))
                    .build();
            currentRow.add(button);

            if (currentRow.size() == columns) {
                rows.add(currentRow);
                currentRow = new ArrayList<>();
            }
        }
        if (!currentRow.isEmpty()) {
            rows.add(currentRow);
        }

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }
}