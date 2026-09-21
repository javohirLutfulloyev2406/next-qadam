package uz.nextqadam.bot.user;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import uz.nextqadam.bot.common.LocalizationService;
import uz.nextqadam.bot.common.MessageTemplateService;
import uz.nextqadam.bot.common.enums.Language;
import uz.nextqadam.bot.common.enums.ToneType;
import uz.nextqadam.bot.common.keyboard.KeyboardService;
import uz.nextqadam.bot.common.telegram.TelegramExecutor;

@Component
public class OnboardingHandler {

    private static final String LANGUAGE_CALLBACK_PREFIX = "LANG_";
    private static final String TONE_CALLBACK_PREFIX = "ONBOARDING_TONE_";

    private final UserService userService;
    private final KeyboardService keyboardService;
    private final TelegramExecutor telegramExecutor;
    private final MessageTemplateService messageTemplateService;
    private final LocalizationService localizationService;

    // TODO: bu holat xotirasi hozircha in-memory Map orqali saqlanmoqda (bir nechta instance/qayta ishga tushirishda
    // yo'qoladi) — keyinchalik Redis yoki DB (masalan alohida "onboarding_state" jadvali) ga ko'chirish kerak.
    private final Map<Long, OnboardingStage> stageByChatId = new ConcurrentHashMap<>();

    public OnboardingHandler(UserService userService, KeyboardService keyboardService, TelegramExecutor telegramExecutor,
                              MessageTemplateService messageTemplateService, LocalizationService localizationService) {
        this.userService = userService;
        this.keyboardService = keyboardService;
        this.telegramExecutor = telegramExecutor;
        this.messageTemplateService = messageTemplateService;
        this.localizationService = localizationService;
    }

    public boolean isAwaitingName(Long chatId) {
        return stageByChatId.get(chatId) == OnboardingStage.AWAITING_NAME;
    }

    public boolean isAwaitingTone(Long chatId) {
        return stageByChatId.get(chatId) == OnboardingStage.AWAITING_TONE;
    }

    public boolean isLanguageCallback(String callbackData) {
        return callbackData != null && callbackData.startsWith(LANGUAGE_CALLBACK_PREFIX);
    }

    public boolean isToneCallback(String callbackData) {
        return callbackData != null && callbackData.startsWith(TONE_CALLBACK_PREFIX);
    }

    /**
     * ResetHandler.softReset oqimidan keyin chaqiriladi — foydalanuvchini xuddi /start birinchi
     * marta bosilgandek qaytadan ism so'rash bosqichiga qaytaradi. handleStart'dagi yangi
     * foydalanuvchi oqimidan farqi shu — User qatorining o'zi allaqachon mavjud (faqat bo'shatilgan),
     * shu sababli handleName endi createUser emas, updateName chaqiradi. Til tanlovi soft reset'da
     * o'zgarmaydi (ResetServiceImpl.softReset language'ga tegmaydi), shu sababli bu yerda til qayta
     * so'ralmaydi — chaqiruvchi mavjud language'ni beradi.
     */
    public void restartOnboarding(Long chatId, Language language) {
        stageByChatId.put(chatId, OnboardingStage.AWAITING_NAME);
        telegramExecutor.sendMessage(chatId, localizationService.get(language, "onboarding.restart"));
    }

    /**
     * StateCleanupService orqali chaqiriladi — masalan ResetHandler.hardDelete'dan keyin, shu
     * chatId uchun qolib ketishi mumkin bo'lgan eski AWAITING_* holatni tozalash uchun.
     */
    public void clearState(Long chatId) {
        stageByChatId.remove(chatId);
    }

    public void handleStart(Update update) {
        Message message = update.getMessage();
        Long chatId = message.getChatId();

        userService.findByTelegramId(chatId).ifPresentOrElse(
                user -> {
                    stageByChatId.remove(chatId);
                    telegramExecutor.sendMessageWithReplyKeyboard(chatId,
                            localizationService.get(user.getLanguage(), "onboarding.welcome.back", user.getName()),
                            keyboardService.buildMainMenuKeyboard(user.getLanguage()));
                },
                () -> {
                    stageByChatId.put(chatId, OnboardingStage.AWAITING_LANGUAGE);
                    telegramExecutor.sendMessageWithKeyboard(chatId,
                            localizationService.get(Language.UZ, "onboarding.ask.language"),
                            keyboardService.buildLanguageChoiceKeyboard(LANGUAGE_CALLBACK_PREFIX));
                }
        );
    }

    public void handleLanguageSelection(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();
        String data = callbackQuery.getData();

        Language language = Language.valueOf(data.substring(LANGUAGE_CALLBACK_PREFIX.length()));

        User user = userService.findByTelegramId(chatId)
                .orElseGet(() -> userService.createUserWithLanguage(chatId, language));
        if (user.getLanguage() != language) {
            user = userService.updateLanguage(user.getId(), language);
        }

        stageByChatId.put(chatId, OnboardingStage.AWAITING_NAME);
        telegramExecutor.sendMessage(chatId, localizationService.get(language, "onboarding.welcome.name"));
    }

    public void handleName(Update update) {
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        String name = message.getText().trim();

        // softReset'dan keyingi qayta-onboarding oqimida yoki til tanlash bosqichidan keyin User
        // qatori allaqachon mavjud (faqat bo'shatilgan yoki name=null) — bunday holatda createUser
        // telegramId unique cheklovini buzadi, shu sababli mavjud bo'lsa yangilaymiz, bo'lmasa
        // (nazariy jihatdan bo'lmasligi kerak, lekin himoya sifatida) yangi qator yaratamiz.
        User user = userService.findByTelegramId(chatId)
                .map(existingUser -> userService.updateName(existingUser.getId(), name))
                .orElseGet(() -> userService.createUser(chatId, name));
        stageByChatId.put(chatId, OnboardingStage.AWAITING_TONE);

        telegramExecutor.sendMessageWithKeyboard(chatId,
                localizationService.get(user.getLanguage(), "onboarding.ask.tone", name),
                keyboardService.buildToneSelectionKeyboard(TONE_CALLBACK_PREFIX, user.getLanguage()));
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
                    messageTemplateService.welcomeAfterTone(user.getLanguage(), tone, user.getName()),
                    keyboardService.buildMainMenuKeyboard(user.getLanguage()));
            telegramExecutor.sendMessageWithKeyboard(chatId,
                    localizationService.get(user.getLanguage(), "onboarding.guide.prompt"),
                    keyboardService.buildGuideLinkKeyboard(user.getLanguage()));
        });
    }

    private enum OnboardingStage {
        AWAITING_LANGUAGE,
        AWAITING_NAME,
        AWAITING_TONE
    }
}
