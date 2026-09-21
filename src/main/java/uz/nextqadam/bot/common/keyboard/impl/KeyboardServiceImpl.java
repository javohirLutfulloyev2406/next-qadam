package uz.nextqadam.bot.common.keyboard.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;

import uz.nextqadam.bot.common.LocalizationService;
import uz.nextqadam.bot.common.enums.Language;
import uz.nextqadam.bot.common.enums.ToneType;
import uz.nextqadam.bot.common.keyboard.KeyboardService;

@Service
public class KeyboardServiceImpl implements KeyboardService {

    private static final int MEMORY_BUTTON_LABEL_MAX_LENGTH = 30;
    private static final int CHECKIN_BUTTON_LABEL_MAX_LENGTH = 30;
    private static final int MAX_TODAY_PRIORITIES = 3;
    private static final String GUIDE_URL = "https://tinyurl.com/czed3pm4";

    private static final String TONE_KEY_PREFIX = "tone.button.";

    private final LocalizationService localizationService;

    public KeyboardServiceImpl(LocalizationService localizationService) {
        this.localizationService = localizationService;
    }

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
    public InlineKeyboardMarkup buildLanguageChoiceKeyboard(String callbackPrefix) {
        List<String> labels = new ArrayList<>();
        List<String> callbackData = new ArrayList<>();
        for (Language lang : Language.values()) {
            labels.add(lang.getDisplayName());
            callbackData.add(callbackPrefix + lang.name());
        }
        return createInlineKeyboard(labels, callbackData, Language.values().length);
    }

    @Override
    public ReplyKeyboardMarkup buildMainMenuKeyboard(Language language) {
        KeyboardRow row1 = new KeyboardRow();
        row1.add(text(language, "menu.newgoal"));
        row1.add(text(language, "menu.nextstep"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(text(language, "menu.done"));
        row2.add(text(language, "menu.profile"));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(text(language, "menu.planday"));
        row3.add(text(language, "menu.braindump"));

        KeyboardRow row4 = new KeyboardRow();
        row4.add(text(language, "menu.motivate"));
        row4.add(text(language, "menu.sos"));

        KeyboardRow row5 = new KeyboardRow();
        row5.add(text(language, "menu.eveningcheckin"));

        return ReplyKeyboardMarkup.builder()
                .keyboardRow(row1)
                .keyboardRow(row2)
                .keyboardRow(row3)
                .keyboardRow(row4)
                .keyboardRow(row5)
                .resizeKeyboard(true)
                .build();
    }

    @Override
    public InlineKeyboardMarkup buildToneSelectionKeyboard(String callbackPrefix, Language language) {
        List<String> labels = new ArrayList<>();
        List<String> callbackData = new ArrayList<>();
        for (ToneType tone : ToneType.values()) {
            labels.add(text(language, TONE_KEY_PREFIX + tone.name().toLowerCase()));
            callbackData.add(callbackPrefix + tone);
        }
        return createInlineKeyboard(labels, callbackData, 2);
    }

    @Override
    public InlineKeyboardMarkup buildProfileMenuKeyboard(Language language) {
        List<List<InlineKeyboardButton>> rows = List.of(
                List.of(button(text(language, "keyboard.profile.edit_name"), "PROFILE_EDIT_NAME"),
                        button(text(language, "keyboard.profile.edit_tone"), "PROFILE_EDIT_TONE")),
                List.of(button(text(language, "keyboard.profile.edit_timezone"), "PROFILE_EDIT_TIMEZONE"),
                        button(text(language, "keyboard.profile.memory"), "MEMORY_VIEW")),
                List.of(button(text(language, "keyboard.profile.language"), "PROFILE_LANGUAGE_ENTRY")),
                List.of(button(text(language, "keyboard.profile.reset"), "PROFILE_RESET_ENTRY"))
        );
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    @Override
    public InlineKeyboardMarkup buildMemoryListKeyboard(List<MemoryListOption> items, Language language) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (MemoryListOption item : items) {
            String label = truncate(item.label(), MEMORY_BUTTON_LABEL_MAX_LENGTH);
            rows.add(List.of(button(label, "MEMORY_DELETE_" + item.id())));
        }
        rows.add(List.of(button(text(language, "keyboard.memory.delete_all"), "MEMORY_DELETE_ALL_ASK")));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    @Override
    public InlineKeyboardMarkup buildConfirmDeleteAllKeyboard(Language language) {
        List<List<InlineKeyboardButton>> rows = List.of(List.of(
                button(text(language, "keyboard.confirm.yes_delete"), "MEMORY_DELETE_ALL_CONFIRM"),
                button(text(language, "keyboard.cancel"), "MEMORY_DELETE_ALL_CANCEL")
        ));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    @Override
    public InlineKeyboardMarkup buildTaskActionKeyboard(UUID taskId, Language language) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(List.of(
                        button(text(language, "keyboard.task.done"), "TASK_DONE_" + taskId),
                        button(text(language, "keyboard.task.snooze"), "TASK_SNOOZE_" + taskId)
                )))
                .build();
    }

    @Override
    public InlineKeyboardMarkup buildGoalsNextStepKeyboard(UUID goalId, Language language) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(List.of(button(text(language, "keyboard.goals.nextstep"), "GOALS_NEXTSTEP_" + goalId))))
                .build();
    }

    @Override
    public InlineKeyboardMarkup buildMorningCheckinKeyboard(List<CheckinTaskOption> options, Language language) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        int selectedCount = 0;
        for (CheckinTaskOption option : options) {
            String checkbox = option.selected() ? "☑️ " : "⬜ ";
            rows.add(List.of(button(checkbox + truncate(option.title(), CHECKIN_BUTTON_LABEL_MAX_LENGTH),
                    "CHECKIN_TOGGLE_" + option.id())));
            if (option.selected()) {
                selectedCount++;
            }
        }
        rows.add(List.of(button(
                localizationService.get(language, "keyboard.checkin.confirm", selectedCount, MAX_TODAY_PRIORITIES),
                "CHECKIN_CONFIRM")));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    @Override
    public InlineKeyboardMarkup buildGoalDriftKeyboard(Language language) {
        List<List<InlineKeyboardButton>> rows = List.of(List.of(
                button(text(language, "keyboard.drift.update_goal"), "DRIFT_UPDATE_GOAL"),
                button(text(language, "keyboard.drift.dismiss"), "DRIFT_DISMISS")
        ));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    @Override
    public InlineKeyboardMarkup buildGuideLinkKeyboard(Language language) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(List.of(webAppButton(text(language, "keyboard.guide.link"), GUIDE_URL))))
                .build();
    }

    @Override
    public InlineKeyboardMarkup buildAdminMenuKeyboard(Language language) {
        List<List<InlineKeyboardButton>> rows = List.of(
                List.of(button(text(language, "keyboard.admin.stats"), "ADMIN_STATS"),
                        button(text(language, "keyboard.admin.broadcast"), "ADMIN_BROADCAST_START")),
                List.of(button(text(language, "keyboard.admin.search"), "ADMIN_SEARCH_START"),
                        button(text(language, "keyboard.admin.errors"), "ADMIN_ERRORS")),
                List.of(button(text(language, "keyboard.admin.refresh"), "ADMIN_REFRESH"))
        );
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    @Override
    public InlineKeyboardMarkup buildAdminBroadcastConfirmKeyboard(Language language) {
        List<List<InlineKeyboardButton>> rows = List.of(List.of(
                button(text(language, "keyboard.admin.broadcast_confirm"), "ADMIN_BROADCAST_CONFIRM"),
                button(text(language, "keyboard.cancel"), "ADMIN_BROADCAST_CANCEL")
        ));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    @Override
    public InlineKeyboardMarkup buildResetChoiceKeyboard(Language language) {
        List<List<InlineKeyboardButton>> rows = List.of(
                List.of(button(text(language, "keyboard.reset.soft"), "RESET_SOFT_ASK")),
                List.of(button(text(language, "keyboard.reset.hard"), "RESET_HARD_ASK"))
        );
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    @Override
    public InlineKeyboardMarkup buildResetSoftConfirmKeyboard(Language language) {
        List<List<InlineKeyboardButton>> rows = List.of(List.of(
                button(text(language, "keyboard.reset.soft_confirm"), "RESET_SOFT_CONFIRM"),
                button(text(language, "keyboard.cancel"), "RESET_CANCEL")
        ));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    @Override
    public InlineKeyboardMarkup buildResetHardConfirmKeyboard(Language language) {
        List<List<InlineKeyboardButton>> rows = List.of(List.of(
                button(text(language, "keyboard.reset.hard_confirm"), "RESET_HARD_CONFIRM"),
                button(text(language, "keyboard.cancel"), "RESET_CANCEL")
        ));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private String text(Language language, String key) {
        return localizationService.get(language, key);
    }

    private InlineKeyboardButton button(String label, String callbackData) {
        return InlineKeyboardButton.builder().text(label).callbackData(callbackData).build();
    }

    private InlineKeyboardButton webAppButton(String label, String url) {
        return InlineKeyboardButton.builder().text(label).webApp(new WebAppInfo(url)).build();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 1) + "…";
    }
}
