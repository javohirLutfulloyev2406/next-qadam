package uz.nextqadam.bot.track;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import uz.nextqadam.bot.ai.dto.EveningCheckinResult;
import uz.nextqadam.bot.ai.dto.GoalDriftResult;
import uz.nextqadam.bot.ai.dto.TaskClassification;
import uz.nextqadam.bot.ai.dto.WeeklyRetrospective;
import uz.nextqadam.bot.common.HtmlEscaper;
import uz.nextqadam.bot.common.keyboard.KeyboardService;
import uz.nextqadam.bot.common.telegram.TelegramExecutor;
import uz.nextqadam.bot.goal.GoalHandler;
import uz.nextqadam.bot.goal.Task;
import uz.nextqadam.bot.goal.TaskRepository;
import uz.nextqadam.bot.user.User;
import uz.nextqadam.bot.user.UserService;

@Component
public class TrackHandler {

    private static final String TYPING_ACTION = "typing";
    private static final String NOT_REGISTERED_MESSAGE = "Avval /start orqali ro'yxatdan o'ting.";
    private static final String DRIFT_UPDATE_GOAL_CALLBACK = "DRIFT_UPDATE_GOAL";
    private static final String DRIFT_DISMISS_CALLBACK = "DRIFT_DISMISS";
    private static final int DRIFT_THRESHOLD = 50;

    // TODO: xuddi boshqa handler'lardagi kabi — in-memory xotira, instance qayta ishga tushirilganda
    // yo'qoladi, keyinchalik Redis/DB'ga ko'chirish kerak.
    private final Map<Long, TrackStage> stageByChatId = new ConcurrentHashMap<>();

    private final TrackService trackService;
    private final UserService userService;
    private final GoalHandler goalHandler;
    private final TaskRepository taskRepository;
    private final KeyboardService keyboardService;
    private final TelegramExecutor telegramExecutor;

    public TrackHandler(TrackService trackService, UserService userService, GoalHandler goalHandler,
                         TaskRepository taskRepository, KeyboardService keyboardService,
                         TelegramExecutor telegramExecutor) {
        this.trackService = trackService;
        this.userService = userService;
        this.goalHandler = goalHandler;
        this.taskRepository = taskRepository;
        this.keyboardService = keyboardService;
        this.telegramExecutor = telegramExecutor;
    }

    public boolean isAwaitingEveningCheckinText(Long chatId) {
        return stageByChatId.get(chatId) == TrackStage.AWAITING_EVENING_CHECKIN_TEXT;
    }

    /**
     * StateCleanupService orqali chaqiriladi (masalan ResetHandler'dan keyin) — shu chatId uchun
     * qolib ketgan AWAITING_EVENING_CHECKIN_TEXT holatini tozalaydi.
     */
    public void clearState(Long chatId) {
        stageByChatId.remove(chatId);
    }

    public void handleEveningCheckinCommand(Update update) {
        Long chatId = update.getMessage().getChatId();
        stageByChatId.put(chatId, TrackStage.AWAITING_EVENING_CHECKIN_TEXT);
        telegramExecutor.sendMessage(chatId,
                "🌙 <b>Kun qanday o'tdi?</b>\n\n"
                        + "Erkin yozing — nima qildingiz, nima qolib ketdi, sabablarini ham aytishingiz "
                        + "mumkin. Men tushunib, hisobga olaman.");
    }

    public void handleEveningCheckinText(Update update) {
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        String rawText = message.getText().trim();
        stageByChatId.remove(chatId);

        User user = userService.findByTelegramId(chatId).orElse(null);
        if (user == null) {
            telegramExecutor.sendMessage(chatId, NOT_REGISTERED_MESSAGE);
            return;
        }

        telegramExecutor.sendChatAction(chatId, TYPING_ACTION);
        EveningCheckinResult result = trackService.processEveningCheckin(user.getId(), rawText);

        if (result == null) {
            telegramExecutor.sendMessage(chatId, "Rahmat, qayd etib qo'ydim 🙂");
            return;
        }

        telegramExecutor.sendMessage(chatId, formatEveningCheckinResult(result));
    }

    public void handleTestRetroCommand(Update update) {
        Long chatId = update.getMessage().getChatId();
        User user = userService.findByTelegramId(chatId).orElse(null);
        if (user == null) {
            telegramExecutor.sendMessage(chatId, NOT_REGISTERED_MESSAGE);
            return;
        }

        telegramExecutor.sendChatAction(chatId, TYPING_ACTION);
        WeeklyRetrospective retrospective = trackService.generateWeeklyRetrospective(user.getId());
        String text = "📊 <b>Haftalik xulosa</b>\n\n" + HtmlEscaper.escape(retrospective.narrative())
                + "\n\n💡 <b>Keyingi hafta uchun:</b> " + HtmlEscaper.escape(retrospective.recommendation());
        telegramExecutor.sendMessage(chatId, text);
    }

    public boolean isDriftUpdateGoalCallback(String callbackData) {
        return DRIFT_UPDATE_GOAL_CALLBACK.equals(callbackData);
    }

    public boolean isDriftDismissCallback(String callbackData) {
        return DRIFT_DISMISS_CALLBACK.equals(callbackData);
    }

    public void handleTestDriftCommand(Update update) {
        Long chatId = update.getMessage().getChatId();
        User user = userService.findByTelegramId(chatId).orElse(null);
        if (user == null) {
            telegramExecutor.sendMessage(chatId, NOT_REGISTERED_MESSAGE);
            return;
        }

        telegramExecutor.sendChatAction(chatId, TYPING_ACTION);
        GoalDriftResult drift = trackService.checkGoalDrift(user.getId());
        if (drift.matchScore() < DRIFT_THRESHOLD) {
            sendDriftWarning(chatId, drift);
        } else {
            telegramExecutor.sendMessage(chatId,
                    "✅ Yo'nalishingiz to'g'ri, " + drift.matchScore() + "/100 moslik.");
        }
    }

    /**
     * RetrospectiveScheduler tomonidan ishlatiladi — faqat moslik past bo'lsa ogohlantirish yuboradi,
     * aks holda jim turadi (haftalik xulosadan tashqari qo'shimcha xabar yuborilmaydi).
     */
    public void sendDriftWarningIfBelowThreshold(Long chatId, GoalDriftResult drift) {
        if (drift.matchScore() < DRIFT_THRESHOLD) {
            sendDriftWarning(chatId, drift);
        }
    }

    public void handleDriftUpdateGoalCallback(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        goalHandler.beginGoalDescriptionFlow(chatId);
        telegramExecutor.editMessageReplyMarkup(chatId, messageId, InlineKeyboardMarkup.builder().keyboard(List.of()).build());
        telegramExecutor.sendMessage(chatId, "Yangi yo'nalishingizni yozing — /newgoal orqali qaytadan boshlaymiz 🎯");
    }

    public void handleDriftDismissCallback(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        telegramExecutor.editMessageText(chatId, messageId, "Tushunarli, davom etamiz 👍");
        telegramExecutor.editMessageReplyMarkup(chatId, messageId, InlineKeyboardMarkup.builder().keyboard(List.of()).build());
    }

    private void sendDriftWarning(Long chatId, GoalDriftResult drift) {
        String text = "🧭 <b>Bir daqiqa to'xtaylik</b>\n\n" + HtmlEscaper.escape(drift.explanation())
                + "\n\nYo'nalishni qayta ko'rib chiqishni xohlaysizmi?";
        telegramExecutor.sendMessageWithKeyboard(chatId, text, keyboardService.buildGoalDriftKeyboard());
    }

    private String formatEveningCheckinResult(EveningCheckinResult result) {
        StringBuilder sb = new StringBuilder("📋 <b>Bugungi natija</b>\n\n");
        for (TaskClassification classification : result.classifications()) {
            String title = taskRepository.findById(classification.taskId()).map(Task::getTitle).orElse("?");
            if (classification.done()) {
                sb.append("✅ ").append(HtmlEscaper.escape(title)).append("\n");
            } else {
                sb.append("❌ ").append(HtmlEscaper.escape(title)).append(" — ")
                        .append(HtmlEscaper.escape(classification.reason())).append("\n");
            }
        }

        if (result.overallMood() != null && !result.overallMood().isBlank()) {
            sb.append("\n💭 ").append(HtmlEscaper.escape(result.overallMood())).append("\n");
        }

        sb.append("\nErtaga yangi imkoniyat 🌅");
        return sb.toString();
    }

    private enum TrackStage {
        AWAITING_EVENING_CHECKIN_TEXT
    }
}
