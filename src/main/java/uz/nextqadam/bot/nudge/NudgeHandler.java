package uz.nextqadam.bot.nudge;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import uz.nextqadam.bot.common.MessageTemplateService;
import uz.nextqadam.bot.common.enums.ToneType;
import uz.nextqadam.bot.common.keyboard.KeyboardService;
import uz.nextqadam.bot.common.telegram.TelegramExecutor;
import uz.nextqadam.bot.goal.GoalHandler;
import uz.nextqadam.bot.goal.Task;
import uz.nextqadam.bot.plan.PlanService;
import uz.nextqadam.bot.user.User;
import uz.nextqadam.bot.user.UserService;

@Component
public class NudgeHandler {

    private static final String TASK_SNOOZE_CALLBACK_PREFIX = "TASK_SNOOZE_";
    private static final String NOT_REGISTERED_MESSAGE = "Avval /start orqali ro'yxatdan o'ting.";

    private final NudgeService nudgeService;
    private final PlanService planService;
    private final UserService userService;
    private final GoalHandler goalHandler;
    private final KeyboardService keyboardService;
    private final MessageTemplateService messageTemplateService;
    private final TelegramExecutor telegramExecutor;

    public NudgeHandler(NudgeService nudgeService, PlanService planService, UserService userService,
                         GoalHandler goalHandler, KeyboardService keyboardService,
                         MessageTemplateService messageTemplateService, TelegramExecutor telegramExecutor) {
        this.nudgeService = nudgeService;
        this.planService = planService;
        this.userService = userService;
        this.goalHandler = goalHandler;
        this.keyboardService = keyboardService;
        this.messageTemplateService = messageTemplateService;
        this.telegramExecutor = telegramExecutor;
    }

    public boolean isTaskSnoozeCallback(String callbackData) {
        return callbackData != null && callbackData.startsWith(TASK_SNOOZE_CALLBACK_PREFIX);
    }

    public void handleSnoozeCallback(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        UUID taskId = UUID.fromString(callbackQuery.getData().substring(TASK_SNOOZE_CALLBACK_PREFIX.length()));

        ToneType tone = userService.findByTelegramId(chatId).map(User::getTonePreference).orElse(ToneType.NORMAL);
        SnoozeResult result = nudgeService.snoozeTask(taskId);

        if (!result.adaptiveShrinkApplied()) {
            telegramExecutor.editMessageText(chatId, messageId, messageTemplateService.snoozeAck(tone));
            telegramExecutor.editMessageReplyMarkup(chatId, messageId,
                    InlineKeyboardMarkup.builder().keyboard(List.of()).build());
            return;
        }

        telegramExecutor.editMessageText(chatId, messageId,
                messageTemplateService.adaptiveShrinkNotice(tone, result.newEstimatedMinutes()));
        telegramExecutor.editMessageReplyMarkup(chatId, messageId,
                InlineKeyboardMarkup.builder().keyboard(List.of()).build());

        Task task = result.task();
        telegramExecutor.sendMessageWithKeyboard(chatId, goalHandler.buildTaskCardMessage(task),
                keyboardService.buildTaskActionKeyboard(task.getId()));
    }

    /**
     * Kunlik 16:00 nudge'ning bitta foydalanuvchi uchun logikasi — DailyPriorityNudgeScheduler va
     * /testnudge (handleTestNudgeCommand) tomonidan bir xil ishlatiladi.
     */
    public void sendPriorityNudgeIfAny(User user) {
        planService.getTodayPriorityTasks(user.getId()).stream().findFirst().ifPresent(task ->
                telegramExecutor.sendMessageWithKeyboard(user.getTelegramId(),
                        messageTemplateService.reminderNudge(user.getTonePreference(), task.getTitle()),
                        keyboardService.buildTaskActionKeyboard(task.getId())));
    }

    public void handleTestNudgeCommand(Update update) {
        Long chatId = update.getMessage().getChatId();
        User user = userService.findByTelegramId(chatId).orElse(null);
        if (user == null) {
            telegramExecutor.sendMessage(chatId, NOT_REGISTERED_MESSAGE);
            return;
        }
        sendPriorityNudgeIfAny(user);
    }
}
