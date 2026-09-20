package uz.nextqadam.bot.user;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import uz.nextqadam.bot.common.enums.ToneType;
import uz.nextqadam.bot.common.keyboard.KeyboardService;
import uz.nextqadam.bot.common.telegram.TelegramExecutor;
import uz.nextqadam.bot.common.util.TimeUtil;
import uz.nextqadam.bot.goal.GoalService;

@Component
public class ProfileHandler {

    private static final String TONE_CALLBACK_PREFIX = "PROFILE_TONE_";
    private static final String EDIT_NAME_CALLBACK = "PROFILE_EDIT_NAME";
    private static final String EDIT_TONE_CALLBACK = "PROFILE_EDIT_TONE";
    private static final String EDIT_TIMEZONE_CALLBACK = "PROFILE_EDIT_TIMEZONE";

    private static final Map<ToneType, String> TONE_DISPLAY = Map.of(
            ToneType.SOFT, "🌱 Yumshoq",
            ToneType.NORMAL, "📋 Oddiy",
            ToneType.HARD, "🔥 Qattiq",
            ToneType.HARDCORE, "⚡ Hardcore"
    );

    // TODO: xuddi OnboardingHandler/GoalHandler'dagi kabi — in-memory xotira, instance qayta ishga
    // tushirilganda yo'qoladi, keyinchalik Redis/DB'ga ko'chirish kerak.
    private final Map<Long, ProfileStage> stageByChatId = new ConcurrentHashMap<>();

    private final UserService userService;
    private final GoalService goalService;
    private final KeyboardService keyboardService;
    private final TelegramExecutor telegramExecutor;

    public ProfileHandler(UserService userService, GoalService goalService, KeyboardService keyboardService,
                           TelegramExecutor telegramExecutor) {
        this.userService = userService;
        this.goalService = goalService;
        this.keyboardService = keyboardService;
        this.telegramExecutor = telegramExecutor;
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

    public void handleProfileCommand(Update update) {
        showProfile(update.getMessage().getChatId());
    }

    public void handleEditNameCallback(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        stageByChatId.put(chatId, ProfileStage.AWAITING_NAME);
        telegramExecutor.sendMessage(chatId, "Yangi ismingizni yozing:");
    }

    public void handleEditToneCallback(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        telegramExecutor.sendMessageWithKeyboard(chatId, "Yangi uslubni tanlang:",
                keyboardService.buildToneSelectionKeyboard(TONE_CALLBACK_PREFIX));
    }

    public void handleEditTimezoneCallback(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        stageByChatId.put(chatId, ProfileStage.AWAITING_TIMEZONE);
        telegramExecutor.sendMessage(chatId, "Vaqt zonangizni yozing (masalan: Asia/Tashkent yoki +5):");
    }

    public void handleToneCallback(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();
        ToneType tone = ToneType.valueOf(callbackQuery.getData().substring(TONE_CALLBACK_PREFIX.length()));

        userService.findByTelegramId(chatId).ifPresent(user -> {
            userService.updateTonePreference(user.getId(), tone);
            telegramExecutor.sendMessage(chatId, "✅ Uslub yangilandi: " + TONE_DISPLAY.get(tone));
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
            telegramExecutor.sendMessage(chatId, "✅ Ism yangilandi: " + newName);
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
            telegramExecutor.sendMessage(chatId, "✅ Vaqt zonasi yangilandi: " + timezone);
        });
        showProfile(chatId);
    }

    private void showProfile(Long chatId) {
        userService.findByTelegramId(chatId).ifPresentOrElse(
                user -> {
                    int activeGoalsCount = goalService.getActiveGoals(user.getId()).size();
                    String timezone = user.getTimezone() != null ? user.getTimezone() : "belgilanmagan";

                    String text = "👤 <b>" + user.getName() + "</b>\n\n"
                            + "🎭 Uslub: " + TONE_DISPLAY.get(user.getTonePreference()) + "\n"
                            + "🌍 Vaqt zonasi: " + timezone + "\n"
                            + "📅 A'zo bo'lgan: " + TimeUtil.formatDateOnly(user.getCreatedAt()) + "\n"
                            + "🎯 Faol maqsadlar: " + activeGoalsCount + "\n\n"
                            + "Nimani o'zgartirmoqchisiz?";

                    telegramExecutor.sendMessageWithKeyboard(chatId, text, keyboardService.buildProfileMenuKeyboard());
                },
                () -> telegramExecutor.sendMessage(chatId, "Avval /start orqali ro'yxatdan o'ting.")
        );
    }

    private enum ProfileStage {
        AWAITING_NAME,
        AWAITING_TIMEZONE
    }
}
