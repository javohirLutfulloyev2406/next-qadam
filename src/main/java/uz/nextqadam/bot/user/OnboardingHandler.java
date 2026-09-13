package uz.nextqadam.bot.user;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import uz.nextqadam.bot.common.enums.ToneType;
import uz.nextqadam.bot.common.keyboard.KeyboardService;
import uz.nextqadam.bot.common.telegram.TelegramExecutor;

@Component
public class OnboardingHandler {

    private static final String TONE_CALLBACK_PREFIX = "ONBOARDING_TONE_";

    private final UserService userService;
    private final KeyboardService keyboardService;
    private final TelegramExecutor telegramExecutor;

    // TODO: bu holat xotirasi hozircha in-memory Map orqali saqlanmoqda (bir nechta instance/qayta ishga tushirishda
    // yo'qoladi) — keyinchalik Redis yoki DB (masalan alohida "onboarding_state" jadvali) ga ko'chirish kerak.
    private final Map<Long, OnboardingStage> stageByChatId = new ConcurrentHashMap<>();

    public OnboardingHandler(UserService userService, KeyboardService keyboardService, TelegramExecutor telegramExecutor) {
        this.userService = userService;
        this.keyboardService = keyboardService;
        this.telegramExecutor = telegramExecutor;
    }

    public boolean isAwaitingName(Long chatId) {
        return stageByChatId.get(chatId) == OnboardingStage.AWAITING_NAME;
    }

    public boolean isAwaitingTone(Long chatId) {
        return stageByChatId.get(chatId) == OnboardingStage.AWAITING_TONE;
    }

    public boolean isToneCallback(String callbackData) {
        return callbackData != null && callbackData.startsWith(TONE_CALLBACK_PREFIX);
    }

    public void handleStart(Update update) {
        Message message = update.getMessage();
        Long chatId = message.getChatId();

        userService.findByTelegramId(chatId).ifPresentOrElse(
                user -> {
                    stageByChatId.remove(chatId);
                    telegramExecutor.sendMessage(chatId, "Yana xush kelibsiz, " + user.getName() + "! 👋");
                },
                () -> {
                    stageByChatId.put(chatId, OnboardingStage.AWAITING_NAME);
                    telegramExecutor.sendMessage(chatId,
                            "Assalomu alaykum! NextQadam botiga xush kelibsiz.\nKeling, tanishaylik — ismingiz nima?");
                }
        );
    }

    public void handleName(Update update) {
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        String name = message.getText().trim();

        userService.createUser(chatId, name);
        stageByChatId.put(chatId, OnboardingStage.AWAITING_TONE);

        List<String> labels = List.of("Yumshoq", "Oddiy", "Qattiq", "Hardcore");
        List<String> callbackData = List.of(
                TONE_CALLBACK_PREFIX + ToneType.SOFT,
                TONE_CALLBACK_PREFIX + ToneType.NORMAL,
                TONE_CALLBACK_PREFIX + ToneType.HARD,
                TONE_CALLBACK_PREFIX + ToneType.HARDCORE
        );

        telegramExecutor.sendMessageWithKeyboard(chatId,
                "Tanishganimdan xursandman, " + name + "! Endi menga qaysi uslubda gaplashishimni tanlang:",
                keyboardService.createInlineKeyboard(labels, callbackData, 2));
    }

    public void handleToneSelection(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();
        String data = callbackQuery.getData();

        ToneType tone = ToneType.valueOf(data.substring(TONE_CALLBACK_PREFIX.length()));

        userService.findByTelegramId(chatId).ifPresent(user -> {
            userService.updateTonePreference(user.getId(), tone);
            stageByChatId.remove(chatId);
            telegramExecutor.sendMessage(chatId,
                    "Ajoyib! Tanlovingiz saqlandi. Endi birinchi maqsadingizni belgilashdan boshlashimiz mumkin. 🚀");
        });
    }

    private enum OnboardingStage {
        AWAITING_NAME,
        AWAITING_TONE
    }
}
