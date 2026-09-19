package uz.nextqadam.bot.user;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import uz.nextqadam.bot.common.MessageTemplateService;
import uz.nextqadam.bot.common.enums.ToneType;
import uz.nextqadam.bot.common.keyboard.KeyboardService;
import uz.nextqadam.bot.common.telegram.TelegramExecutor;

@Component
public class OnboardingHandler {

    private static final String TONE_CALLBACK_PREFIX = "ONBOARDING_TONE_";

    private final UserService userService;
    private final KeyboardService keyboardService;
    private final TelegramExecutor telegramExecutor;
    private final MessageTemplateService messageTemplateService;

    // TODO: bu holat xotirasi hozircha in-memory Map orqali saqlanmoqda (bir nechta instance/qayta ishga tushirishda
    // yo'qoladi) — keyinchalik Redis yoki DB (masalan alohida "onboarding_state" jadvali) ga ko'chirish kerak.
    private final Map<Long, OnboardingStage> stageByChatId = new ConcurrentHashMap<>();

    public OnboardingHandler(UserService userService, KeyboardService keyboardService, TelegramExecutor telegramExecutor,
                              MessageTemplateService messageTemplateService) {
        this.userService = userService;
        this.keyboardService = keyboardService;
        this.telegramExecutor = telegramExecutor;
        this.messageTemplateService = messageTemplateService;
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
                    telegramExecutor.sendMessageWithReplyKeyboard(chatId, "Yana xush kelibsiz, " + user.getName() + "! 👋",
                            keyboardService.buildMainMenuKeyboard());
                },
                () -> {
                    stageByChatId.put(chatId, OnboardingStage.AWAITING_NAME);
                    telegramExecutor.sendMessage(chatId,
                            "👋 Assalomu alaykum! Men NextQadam — katta orzularingni kichik, bajarilishi oson "
                                    + "qadamlarga bo'lib beruvchi shaxsiy hamrohingman.\n\n"
                                    + "Katta reja emas — bugungi bitta qadam. Shu tarzda oldinga siljiymiz. 🚀\n\n"
                                    + "Avval tanishib olaylik — ismingiz nima?");
                }
        );
    }

    public void handleName(Update update) {
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        String name = message.getText().trim();

        userService.createUser(chatId, name);
        stageByChatId.put(chatId, OnboardingStage.AWAITING_TONE);

        telegramExecutor.sendMessageWithKeyboard(chatId,
                "Tanishganimdan xursandman, " + name + "! Endi menga qaysi uslubda gaplashishimni tanlang:\n\n"
                        + "🌱 Yumshoq — iliq va tushunuvchan ohangda qo'llab-quvvatlayman\n"
                        + "📋 Oddiy — sodda va aniq, ortiqcha so'zlarsiz gaplashaman\n"
                        + "🔥 Qattiq — tik va talabchan, gapni aylantirmayman\n"
                        + "⚡ Hardcore — hech narsani yumshatmayman, to'g'ridan-to'g'ri aytaman",
                keyboardService.buildToneSelectionKeyboard(TONE_CALLBACK_PREFIX));
    }

    public void handleToneSelection(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();
        String data = callbackQuery.getData();

        ToneType tone = ToneType.valueOf(data.substring(TONE_CALLBACK_PREFIX.length()));

        userService.findByTelegramId(chatId).ifPresent(user -> {
            userService.updateTonePreference(user.getId(), tone);
            stageByChatId.remove(chatId);
            telegramExecutor.sendMessageWithReplyKeyboard(chatId,
                    messageTemplateService.welcomeAfterTone(tone, user.getName()),
                    keyboardService.buildMainMenuKeyboard());
            telegramExecutor.sendMessageWithKeyboard(chatId,
                    "📖 Aytgancha, botdan to'liq foydalanish uchun qisqa qo'llanma tayyorladik — xohlasangiz "
                            + "ko'rib chiqing:",
                    keyboardService.buildGuideLinkKeyboard());
        });
    }

    private enum OnboardingStage {
        AWAITING_NAME,
        AWAITING_TONE
    }
}
