package uz.nextqadam.bot.common.keyboard.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import uz.nextqadam.bot.common.enums.ToneType;
import uz.nextqadam.bot.common.keyboard.KeyboardService;

@Service
public class KeyboardServiceImpl implements KeyboardService {

    private static final Map<ToneType, String> TONE_BUTTON_LABELS = Map.of(
            ToneType.SOFT, "Yumshoq 🌱",
            ToneType.NORMAL, "Oddiy 📋",
            ToneType.HARD, "Qattiq 🔥",
            ToneType.HARDCORE, "Hardcore ⚡"
    );

    private static final int MEMORY_BUTTON_LABEL_MAX_LENGTH = 30;

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

    @Override
    public ReplyKeyboardMarkup buildMainMenuKeyboard() {
        KeyboardRow row1 = new KeyboardRow();
        row1.add("🎯 Yangi maqsad");
        row1.add("📌 Keyingi qadam");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("✅ Bajardim");
        row2.add("👤 Profil");

        return ReplyKeyboardMarkup.builder()
                .keyboardRow(row1)
                .keyboardRow(row2)
                .resizeKeyboard(true)
                .build();
    }

    @Override
    public InlineKeyboardMarkup buildToneSelectionKeyboard(String callbackPrefix) {
        List<String> labels = new ArrayList<>();
        List<String> callbackData = new ArrayList<>();
        for (ToneType tone : ToneType.values()) {
            labels.add(TONE_BUTTON_LABELS.get(tone));
            callbackData.add(callbackPrefix + tone);
        }
        return createInlineKeyboard(labels, callbackData, 2);
    }

    @Override
    public InlineKeyboardMarkup buildProfileMenuKeyboard() {
        List<List<InlineKeyboardButton>> rows = List.of(
                List.of(button("✏️ Ismni o'zgartirish", "PROFILE_EDIT_NAME"),
                        button("🎭 Uslubni o'zgartirish", "PROFILE_EDIT_TONE")),
                List.of(button("🌍 Vaqt zonasi", "PROFILE_EDIT_TIMEZONE"),
                        button("🧠 Xotiram", "MEMORY_VIEW"))
        );
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    @Override
    public InlineKeyboardMarkup buildMemoryListKeyboard(List<MemoryListOption> items) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (MemoryListOption item : items) {
            String label = truncate(item.label(), MEMORY_BUTTON_LABEL_MAX_LENGTH);
            rows.add(List.of(button(label, "MEMORY_DELETE_" + item.id())));
        }
        rows.add(List.of(button("🗑️ Barchasini o'chirish", "MEMORY_DELETE_ALL_ASK")));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    @Override
    public InlineKeyboardMarkup buildConfirmDeleteAllKeyboard() {
        List<List<InlineKeyboardButton>> rows = List.of(List.of(
                button("✅ Ha, o'chir", "MEMORY_DELETE_ALL_CONFIRM"),
                button("❌ Bekor qilish", "MEMORY_DELETE_ALL_CANCEL")
        ));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private InlineKeyboardButton button(String label, String callbackData) {
        return InlineKeyboardButton.builder().text(label).callbackData(callbackData).build();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 1) + "…";
    }
}