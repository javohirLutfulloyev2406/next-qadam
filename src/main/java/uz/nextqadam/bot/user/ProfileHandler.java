package uz.nextqadam.bot.user;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import uz.nextqadam.bot.common.LocalizationService;
import uz.nextqadam.bot.common.enums.Language;
import uz.nextqadam.bot.common.enums.ToneType;
import uz.nextqadam.bot.common.keyboard.KeyboardService;
import uz.nextqadam.bot.common.telegram.TelegramExecutor;
import uz.nextqadam.bot.common.util.TimeUtil;
import uz.nextqadam.bot.goal.GoalService;

@Component
public class ProfileHandler {

    private static final String TONE_CALLBACK_PREFIX = "PROFILE_TONE_";
    private static final String LANGUAGE_CALLBACK_PREFIX = "PROFILE_LANG_";
    private static final String LANGUAGE_ENTRY_CALLBACK = "PROFILE_LANGUAGE_ENTRY";
    private static final String EDIT_NAME_CALLBACK = "PROFILE_EDIT_NAME";
    private static final String EDIT_TONE_CALLBACK = "PROFILE_EDIT_TONE";
    private static final String EDIT_TIMEZONE_CALLBACK = "PROFILE_EDIT_TIMEZONE";

    // TODO: xuddi OnboardingHandler/GoalHandler'dagi kabi — in-memory xotira, instance qayta ishga
    // tushirilganda yo'qoladi, keyinchalik Redis/DB'ga ko'chirish kerak.
    private final Map<Long, ProfileStage> stageByChatId = new ConcurrentHashMap<>();

    private final UserService userService;
    private final GoalService goalService;
    private final KeyboardService keyboardService;
    private final TelegramExecutor telegramExecutor;
    private final LocalizationService localizationService;

    public ProfileHandler(UserService userService, GoalService goalService, KeyboardService keyboardService,
                           TelegramExecutor telegramExecutor, LocalizationService localizationService) {
        this.userService = userService;
        this.goalService = goalService;
        this.keyboardService = keyboardService;
        this.telegramExecutor = telegramExecutor;
        this.localizationService = localizationService;
    }

    public boolean isAwaitingProfileName(Long chatId) {
        return stageByChatId.get(chatId) == ProfileStage.AWAITING_NAME;
    }

    public boolean isAwaitingProfileTimezone(Long chatId) {
        return stageByChatId.get(chatId) == ProfileStage.AWAITING_TIMEZONE;
    }

    /**
     * StateCleanupService orqali chaqiriladi (masalan ResetHandler'dan keyin) — shu chatId uchun
     * qolib ketgan AWAITING_* holatini tozalaydi.
     */
    public void clearState(Long chatId) {
        stageByChatId.remove(chatId);
    }

    public boolean isEditNameCallback(String callbackData) {
        return EDIT_NAME_CALLBACK.equals(callbackData);
    }

    public boolean isEditToneCallback(String callbackData) {
        return EDIT_TONE_CALLBACK.equals(callbackData);
    }

    public boolean isEditTimezoneCallback(String callbackData) {
        return EDIT_TIMEZONE_CALLBACK.equals(callbackData);
    }

    public boolean isProfileToneCallback(String callbackData) {
        return callbackData != null && callbackData.startsWith(TONE_CALLBACK_PREFIX);
    }

    public boolean isLanguageEntryCallback(String callbackData) {
        return LANGUAGE_ENTRY_CALLBACK.equals(callbackData);
    }

    public boolean isProfileLanguageCallback(String callbackData) {
        return callbackData != null && callbackData.startsWith(LANGUAGE_CALLBACK_PREFIX);
    }

    public void handleProfileCommand(Update update) {
        showProfile(update.getMessage().getChatId());
    }

    public void handleEditNameCallback(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        stageByChatId.put(chatId, ProfileStage.AWAITING_NAME);
        telegramExecutor.sendMessage(chatId, localizationService.get(languageOf(chatId), "profile.ask_name"));
    }

    public void handleEditToneCallback(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Language language = languageOf(chatId);
        telegramExecutor.sendMessageWithKeyboard(chatId, localizationService.get(language, "profile.ask_tone"),
                keyboardService.buildToneSelectionKeyboard(TONE_CALLBACK_PREFIX, language));
    }

    public void handleEditTimezoneCallback(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        stageByChatId.put(chatId, ProfileStage.AWAITING_TIMEZONE);
        telegramExecutor.sendMessage(chatId, localizationService.get(languageOf(chatId), "profile.ask_timezone"));
    }

    public void handleToneCallback(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();
        ToneType tone = ToneType.valueOf(callbackQuery.getData().substring(TONE_CALLBACK_PREFIX.length()));

        userService.findByTelegramId(chatId).ifPresent(user -> {
            userService.updateTonePreference(user.getId(), tone);
            telegramExecutor.sendMessage(chatId,
                    localizationService.get(user.getLanguage(), "profile.tone_updated", toneLabel(tone, user.getLanguage())));
        });
        showProfile(chatId);
    }

    public void handleNameInput(Update update) {
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        String newName = message.getText().trim();
        stageByChatId.remove(chatId);

        userService.findByTelegramId(chatId).ifPresent(user -> {
            userService.updateName(user.getId(), newName);
            telegramExecutor.sendMessage(chatId, localizationService.get(user.getLanguage(), "profile.name_updated", newName));
        });
        showProfile(chatId);
    }

    public void handleTimezoneInput(Update update) {
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        String timezone = message.getText().trim();
        stageByChatId.remove(chatId);

        userService.findByTelegramId(chatId).ifPresent(user -> {
            userService.updateTimezone(user.getId(), timezone);
            telegramExecutor.sendMessage(chatId, localizationService.get(user.getLanguage(), "profile.timezone_updated", timezone));
        });
        showProfile(chatId);
    }

    /**
     * /language komandasi — istalgan vaqtda tilni o'zgartirish. Onboarding'dagi LANG_ prefiksidan
     * PROFILE_LANG_ bilan farqlanadi, chunki bu yerda User allaqachon mavjud va tanlov shu zahoti
     * saqlanishi kerak (onboarding oqimiga aralashmaydi).
     */
    public void handleLanguageCommand(Update update) {
        Long chatId = update.getMessage().getChatId();
        showLanguageChoice(chatId);
    }

    public void handleLanguageEntryCallback(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        showLanguageChoice(chatId);
    }

    private void showLanguageChoice(Long chatId) {
        Language language = languageOf(chatId);
        telegramExecutor.sendMessageWithKeyboard(chatId, localizationService.get(language, "profile.ask_language"),
                keyboardService.buildLanguageChoiceKeyboard(LANGUAGE_CALLBACK_PREFIX));
    }

    public void handleProfileLanguageSelection(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();
        Language newLanguage = Language.valueOf(callbackQuery.getData().substring(LANGUAGE_CALLBACK_PREFIX.length()));

        userService.findByTelegramId(chatId).ifPresent(user -> {
            userService.updateLanguage(user.getId(), newLanguage);
            telegramExecutor.sendMessage(chatId,
                    localizationService.get(newLanguage, "profile.language_updated", newLanguage.getDisplayName()));
        });
        showProfile(chatId);
    }

    private void showProfile(Long chatId) {
        userService.findByTelegramId(chatId).ifPresentOrElse(
                user -> {
                    Language language = user.getLanguage();
                    int activeGoalsCount = goalService.getActiveGoals(user.getId()).size();
                    String timezone = user.getTimezone() != null
                            ? user.getTimezone()
                            : localizationService.get(language, "profile.timezone_unset");

                    String text = "👤 <b>" + user.getName() + "</b>\n\n"
                            + localizationService.get(language, "profile.view.tone", toneLabel(user.getTonePreference(), language)) + "\n"
                            + localizationService.get(language, "profile.view.timezone", timezone) + "\n"
                            + localizationService.get(language, "profile.view.joined", TimeUtil.formatDateOnly(user.getCreatedAt())) + "\n"
                            + localizationService.get(language, "profile.view.active_goals", activeGoalsCount) + "\n\n"
                            + localizationService.get(language, "profile.view.prompt");

                    telegramExecutor.sendMessageWithKeyboard(chatId, text, keyboardService.buildProfileMenuKeyboard(language));
                },
                () -> telegramExecutor.sendMessage(chatId, localizationService.get(Language.UZ, "common.please_start"))
        );
    }

    private String toneLabel(ToneType tone, Language language) {
        return localizationService.get(language, "tone.button." + tone.name().toLowerCase());
    }

    private Language languageOf(Long chatId) {
        return userService.findByTelegramId(chatId).map(User::getLanguage).orElse(Language.UZ);
    }

    private enum ProfileStage {
        AWAITING_NAME,
        AWAITING_TIMEZONE
    }
}
