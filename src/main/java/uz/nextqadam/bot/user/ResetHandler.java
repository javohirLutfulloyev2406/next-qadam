package uz.nextqadam.bot.user;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import uz.nextqadam.bot.common.StateCleanupService;
import uz.nextqadam.bot.common.enums.Language;
import uz.nextqadam.bot.common.keyboard.KeyboardService;
import uz.nextqadam.bot.common.telegram.TelegramExecutor;

@Component
public class ResetHandler {

    private static final String PROFILE_ENTRY_CALLBACK = "PROFILE_RESET_ENTRY";
    private static final String SOFT_ASK_CALLBACK = "RESET_SOFT_ASK";
    private static final String SOFT_CONFIRM_CALLBACK = "RESET_SOFT_CONFIRM";
    private static final String HARD_ASK_CALLBACK = "RESET_HARD_ASK";
    private static final String HARD_CONFIRM_CALLBACK = "RESET_HARD_CONFIRM";
    private static final String CANCEL_CALLBACK = "RESET_CANCEL";

    private static final String RESET_INTRO_TEXT = """
            🔄 <b>Hisobingizni tozalash</b>

            Ikki variant bor:

            <b>Yangidan boshlash</b> — barcha maqsad, vazifa va xotirangiz yashiriladi, bot sizni yangi foydalanuvchidek qayta tanishtiradi. Ma'lumot texnik jihatdan bazada qoladi (masalan xizmat sifatini yaxshilash uchun), lekin sizga va botning hech bir funksiyasiga ko'rinmaydi.

            <b>Butunlay o'chirish</b> — barcha ma'lumotingiz bazadan HAQIQIY o'chiriladi, qaytarib bo'lmaydi.

            Qaysi birini tanlaysiz?""";

    private final ResetService resetService;
    private final StateCleanupService stateCleanupService;
    private final UserService userService;
    private final OnboardingHandler onboardingHandler;
    private final KeyboardService keyboardService;
    private final TelegramExecutor telegramExecutor;

    public ResetHandler(ResetService resetService, StateCleanupService stateCleanupService, UserService userService,
                         OnboardingHandler onboardingHandler, KeyboardService keyboardService,
                         TelegramExecutor telegramExecutor) {
        this.resetService = resetService;
        this.stateCleanupService = stateCleanupService;
        this.userService = userService;
        this.onboardingHandler = onboardingHandler;
        this.keyboardService = keyboardService;
        this.telegramExecutor = telegramExecutor;
    }

    public boolean isProfileResetEntryCallback(String callbackData) {
        return PROFILE_ENTRY_CALLBACK.equals(callbackData);
    }

    public boolean isSoftAskCallback(String callbackData) {
        return SOFT_ASK_CALLBACK.equals(callbackData);
    }

    public boolean isSoftConfirmCallback(String callbackData) {
        return SOFT_CONFIRM_CALLBACK.equals(callbackData);
    }

    public boolean isHardAskCallback(String callbackData) {
        return HARD_ASK_CALLBACK.equals(callbackData);
    }

    public boolean isHardConfirmCallback(String callbackData) {
        return HARD_CONFIRM_CALLBACK.equals(callbackData);
    }

    public boolean isCancelCallback(String callbackData) {
        return CANCEL_CALLBACK.equals(callbackData);
    }

    public void handleResetCommand(Update update) {
        Long chatId = chatIdOf(update);
        Optional<User> user = userService.findByTelegramId(chatId);
        if (user.isEmpty()) {
            telegramExecutor.sendMessage(chatId, "Avval /start orqali ro'yxatdan o'ting.");
            return;
        }
        telegramExecutor.sendMessageWithKeyboard(chatId, RESET_INTRO_TEXT,
                keyboardService.buildResetChoiceKeyboard(user.get().getLanguage()));
    }

    public void handleSoftResetAskCallback(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Language language = userService.findByTelegramId(chatId).map(User::getLanguage).orElse(Language.UZ);
        telegramExecutor.sendMessageWithKeyboard(chatId,
                "🔄 Rostdan ham boshidan boshlaymizmi? Joriy maqsad va vazifalaringiz yashiriladi.",
                keyboardService.buildResetSoftConfirmKeyboard(language));
    }

    public void handleSoftResetConfirmCallback(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Language language = userService.findByTelegramId(chatId)
                .map(user -> {
                    resetService.softReset(user.getId());
                    return user.getLanguage();
                })
                .orElse(Language.UZ);
        // Avval BARCHA modullardagi eski holatni tozalaymiz, so'ng OnboardingHandler o'zining
        // AWAITING_NAME holatini o'rnatadi — aks holda tartib teskari bo'lsa, clearAll shu zahoti
        // o'rnatilgan holatni ham o'chirib qo'yadi.
        stateCleanupService.clearAll(chatId);
        onboardingHandler.restartOnboarding(chatId, language);
    }

    public void handleHardDeleteAskCallback(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Language language = userService.findByTelegramId(chatId).map(User::getLanguage).orElse(Language.UZ);
        telegramExecutor.sendMessageWithKeyboard(chatId,
                "🗑 <b>Diqqat:</b> bu amal QAYTARILMAYDI. Barcha ma'lumotingiz butunlay o'chadi. "
                        + "Rostdan ham davom etamizmi?",
                keyboardService.buildResetHardConfirmKeyboard(language));
    }

    public void handleHardDeleteConfirmCallback(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        userService.findByTelegramId(chatId).ifPresent(user -> resetService.hardDelete(user.getId()));
        stateCleanupService.clearAll(chatId);
        telegramExecutor.sendMessage(chatId,
                "✅ Barcha ma'lumotingiz o'chirildi. Yangi suhbat boshlash uchun istalgan vaqtda /start "
                        + "yozishingiz mumkin.");
    }

    public void handleResetCancelCallback(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        telegramExecutor.editMessageText(chatId, messageId, "Bekor qilindi, hech narsa o'zgarmadi.");
        telegramExecutor.editMessageReplyMarkup(chatId, messageId, InlineKeyboardMarkup.builder().keyboard(List.of()).build());
    }

    private Long chatIdOf(Update update) {
        return update.hasCallbackQuery() ? update.getCallbackQuery().getMessage().getChatId() : update.getMessage().getChatId();
    }
}
