package uz.nextqadam.bot.goal;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import uz.nextqadam.bot.common.MessageTemplateService;
import uz.nextqadam.bot.common.keyboard.KeyboardService;
import uz.nextqadam.bot.common.telegram.TelegramExecutor;
import uz.nextqadam.bot.user.User;
import uz.nextqadam.bot.user.UserService;

@Component
public class GoalHandler {

    private static final String TASK_DONE_CALLBACK_PREFIX = "TASK_DONE_";
    private static final String GOALS_NEXTSTEP_CALLBACK_PREFIX = "GOALS_NEXTSTEP_";
    private static final int PROGRESS_BAR_BLOCKS = 10;
    private static final String ALL_TASKS_DONE_MESSAGE =
            "🎉 <b>Bugungi barcha vazifalar tugadi!</b>\n\nErtaga yangi qadam kutmoqda.";

    // TODO: bu holat xotirasi hozircha in-memory Map orqali saqlanmoqda (bir nechta instance/qayta
    // tushirishda yo'qoladi) — keyinchalik Redis yoki DB (masalan alohida "conversation_state" jadvali) ga
    // ko'chirish kerak.
    private final Map<Long, GoalStage> stageByChatId = new ConcurrentHashMap<>();

    private final GoalService goalService;
    private final UserService userService;
    private final MilestoneRepository milestoneRepository;
    private final TaskRepository taskRepository;
    private final TelegramExecutor telegramExecutor;
    private final KeyboardService keyboardService;
    private final MessageTemplateService messageTemplateService;

    public GoalHandler(GoalService goalService, UserService userService, MilestoneRepository milestoneRepository,
                        TaskRepository taskRepository, TelegramExecutor telegramExecutor, KeyboardService keyboardService,
                        MessageTemplateService messageTemplateService) {
        this.goalService = goalService;
        this.userService = userService;
        this.milestoneRepository = milestoneRepository;
        this.taskRepository = taskRepository;
        this.telegramExecutor = telegramExecutor;
        this.keyboardService = keyboardService;
        this.messageTemplateService = messageTemplateService;
    }

    public boolean isAwaitingGoalDescription(Long chatId) {
        return stageByChatId.get(chatId) == GoalStage.AWAITING_GOAL_DESCRIPTION;
    }

    /**
     * StateCleanupService orqali chaqiriladi (masalan ResetHandler'dan keyin) — shu chatId uchun
     * qolib ketgan AWAITING_GOAL_DESCRIPTION holatini tozalaydi.
     */
    public void clearState(Long chatId) {
        stageByChatId.remove(chatId);
    }

    public boolean isTaskDoneCallback(String callbackData) {
        return callbackData != null && callbackData.startsWith(TASK_DONE_CALLBACK_PREFIX);
    }

    public boolean isGoalsNextStepCallback(String callbackData) {
        return callbackData != null && callbackData.startsWith(GOALS_NEXTSTEP_CALLBACK_PREFIX);
    }

    public void handleNewGoalCommand(Update update) {
        Long chatId = update.getMessage().getChatId();
        beginGoalDescriptionFlow(chatId);
        telegramExecutor.sendMessage(chatId,
                "Katta maqsadingizni bir necha jumla bilan yozing "
                        + "(masalan: \"6 oyda backend developer bo'lmoqchiman\")");
    }

    /**
     * /newgoal bosilgandek AWAITING_GOAL_DESCRIPTION holatini o'rnatadi — boshqa modullar (masalan
     * Goal Drift Detection ogohlantirishidagi "Maqsadni yangilash" tugmasi) foydalanuvchini xabar
     * matnini o'zgartirmasdan shu oqimga yo'naltirishi uchun.
     */
    public void beginGoalDescriptionFlow(Long chatId) {
        stageByChatId.put(chatId, GoalStage.AWAITING_GOAL_DESCRIPTION);
    }

    public void handleGoalDescription(Update update) {
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        String rawDescription = message.getText().trim();

        User user = userService.findByTelegramId(chatId).orElse(null);
        stageByChatId.remove(chatId);
        if (user == null) {
            telegramExecutor.sendMessage(chatId, "Avval /start orqali ro'yxatdan o'ting.");
            return;
        }

        Goal goal = goalService.createGoalWithAiDecomposition(user.getId(), rawDescription);

        if (goal.getDescription() != null && goal.getDescription().contains(GoalService.AI_DECOMPOSITION_FAILURE_MARKER)) {
            telegramExecutor.sendMessageWithReplyKeyboard(chatId,
                    "🎯 Maqsadingiz saqlandi, lekin uni bosqichlarga bo'lishda xatolik yuz berdi. "
                            + "Birozdan so'ng /newgoal orqali qayta urinib ko'ring.",
                    keyboardService.buildMainMenuKeyboard());
            return;
        }

        String intro = messageTemplateService.goalDecompositionIntro(user.getTonePreference());
        telegramExecutor.sendMessageWithReplyKeyboard(chatId, intro + "\n\n" + formatGoalSummary(goal),
                keyboardService.buildMainMenuKeyboard());

        goalService.getNextStep(user.getId())
                .ifPresent(task -> telegramExecutor.sendMessageWithKeyboard(chatId, buildTaskCardMessage(task),
                        keyboardService.buildTaskActionKeyboard(task.getId())));
    }

    public void handleNextStepCommand(Update update) {
        Long chatId = update.getMessage().getChatId();
        User user = userService.findByTelegramId(chatId).orElse(null);
        if (user == null) {
            telegramExecutor.sendMessage(chatId, "Avval /start orqali ro'yxatdan o'ting.");
            return;
        }

        goalService.getNextStep(user.getId()).ifPresentOrElse(
                task -> telegramExecutor.sendMessageWithKeyboard(chatId, buildTaskCardMessage(task),
                        keyboardService.buildTaskActionKeyboard(task.getId())),
                () -> telegramExecutor.sendMessageWithReplyKeyboard(chatId,
                        messageTemplateService.noPendingTask(user.getTonePreference()),
                        keyboardService.buildMainMenuKeyboard())
        );
    }

    public void handleDoneCommand(Update update) {
        Long chatId = update.getMessage().getChatId();
        User user = userService.findByTelegramId(chatId).orElse(null);
        if (user == null) {
            telegramExecutor.sendMessage(chatId, "Avval /start orqali ro'yxatdan o'ting.");
            return;
        }

        goalService.getCurrentTaskForUser(user.getId()).ifPresentOrElse(
                task -> {
                    goalService.markTaskDone(task.getId());
                    telegramExecutor.sendMessageWithReplyKeyboard(chatId,
                            messageTemplateService.taskDoneCongrats(user.getTonePreference(), task.getTitle()),
                            keyboardService.buildMainMenuKeyboard());
                    sendNextStepOrCelebration(chatId, user.getId());
                },
                () -> telegramExecutor.sendMessageWithReplyKeyboard(chatId,
                        messageTemplateService.noPendingTask(user.getTonePreference()),
                        keyboardService.buildMainMenuKeyboard())
        );
    }

    public void handleTaskDoneCallback(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        UUID taskId = UUID.fromString(callbackQuery.getData().substring(TASK_DONE_CALLBACK_PREFIX.length()));

        Task task = goalService.markTaskDone(taskId);

        telegramExecutor.editMessageText(chatId, messageId, "✅ <s>" + task.getTitle() + "</s>\n\nAjoyib ish!");
        telegramExecutor.editMessageReplyMarkup(chatId, messageId,
                InlineKeyboardMarkup.builder().keyboard(List.of()).build());

        User user = userService.findByTelegramId(chatId).orElse(null);
        if (user == null) {
            return;
        }
        sendNextStepOrCelebration(chatId, user.getId());
    }

    public void handleGoalsCommand(Update update) {
        Long chatId = update.getMessage().getChatId();
        User user = userService.findByTelegramId(chatId).orElse(null);
        if (user == null) {
            telegramExecutor.sendMessage(chatId, "Avval /start orqali ro'yxatdan o'ting.");
            return;
        }

        Map<Goal, GoalService.ProgressStats> goalsWithProgress = goalService.getGoalsWithProgress(user.getId());
        if (goalsWithProgress.isEmpty()) {
            telegramExecutor.sendMessageWithReplyKeyboard(chatId,
                    "Hozircha faol maqsadingiz yo'q. /newgoal orqali birinchisini qo'shing 🎯",
                    keyboardService.buildMainMenuKeyboard());
            return;
        }

        for (Map.Entry<Goal, GoalService.ProgressStats> entry : goalsWithProgress.entrySet()) {
            Goal goal = entry.getKey();
            GoalService.ProgressStats stats = entry.getValue();
            telegramExecutor.sendMessageWithKeyboard(chatId, formatGoalProgressBlock(goal, stats),
                    keyboardService.buildGoalsNextStepKeyboard(goal.getId()));
        }
    }

    public void handleGoalsNextStepCallback(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();
        UUID goalId = UUID.fromString(callbackQuery.getData().substring(GOALS_NEXTSTEP_CALLBACK_PREFIX.length()));

        goalService.getNextStepForGoal(goalId).ifPresentOrElse(
                task -> telegramExecutor.sendMessageWithKeyboard(chatId, buildTaskCardMessage(task),
                        keyboardService.buildTaskActionKeyboard(task.getId())),
                () -> telegramExecutor.sendMessage(chatId, "Bu maqsad bo'yicha hozircha faol vazifa yo'q.")
        );
    }

    private void sendNextStepOrCelebration(Long chatId, UUID userId) {
        goalService.getNextStep(userId).ifPresentOrElse(
                nextTask -> telegramExecutor.sendMessageWithKeyboard(chatId, buildTaskCardMessage(nextTask),
                        keyboardService.buildTaskActionKeyboard(nextTask.getId())),
                () -> telegramExecutor.sendMessage(chatId, ALL_TASKS_DONE_MESSAGE)
        );
    }

    /**
     * NudgeHandler kabi boshqa modullar ham (masalan adaptive-shrink'dan keyin qayta ko'rsatishda)
     * bir xil kartochka ko'rinishidan foydalanishi uchun public.
     */
    public String buildTaskCardMessage(Task task) {
        StringBuilder sb = new StringBuilder();
        sb.append("📌 <b>Bugungi qadamingiz</b>\n\n");
        sb.append(task.getTitle()).append("\n");
        sb.append("⏱ Taxminan ").append(task.getEstimatedMinutes()).append(" daqiqa\n\n");
        sb.append("Qaysi bosqichdan: ").append(milestoneTitleOf(task));
        return sb.toString();
    }

    private String milestoneTitleOf(Task task) {
        if (task.getMilestone() == null) {
            return "";
        }
        return milestoneRepository.findById(task.getMilestone().getId())
                .map(Milestone::getTitle)
                .orElse("");
    }

    private String formatGoalSummary(Goal goal) {
        List<Milestone> milestones = milestoneRepository.findAllByGoalId(goal.getId());
        List<Task> tasks = taskRepository.findAllByGoalId(goal.getId());

        StringBuilder sb = new StringBuilder();
        sb.append("🎯 <b>").append(goal.getTitle()).append("</b>\n\n");
        for (Milestone milestone : milestones) {
            long milestoneTaskCount = tasks.stream()
                    .filter(task -> task.getMilestone() != null && task.getMilestone().getId().equals(milestone.getId()))
                    .count();
            sb.append(periodEmoji(milestone.getPeriod())).append(" ").append(milestone.getTitle())
                    .append(" — 0/").append(milestoneTaskCount).append("\n");
        }
        sb.append("\n📊 Jami: ").append(milestones.size()).append(" bosqich, ").append(tasks.size()).append(" vazifa\n\n");
        sb.append("Boshlaymiz! 👇");
        return sb.toString();
    }

    private String formatGoalProgressBlock(Goal goal, GoalService.ProgressStats stats) {
        StringBuilder sb = new StringBuilder();
        sb.append("🎯 <b>").append(goal.getTitle()).append("</b>\n");
        sb.append(buildProgressBar(stats.percentComplete())).append(" ").append(stats.percentComplete()).append("%\n");
        sb.append(stats.doneCount()).append("/").append(stats.totalCount()).append(" vazifa bajarildi");
        return sb.toString();
    }

    private String buildProgressBar(int percentComplete) {
        int filledBlocks = Math.max(0, Math.min(PROGRESS_BAR_BLOCKS, Math.round(percentComplete * PROGRESS_BAR_BLOCKS / 100f)));
        return "█".repeat(filledBlocks) + "░".repeat(PROGRESS_BAR_BLOCKS - filledBlocks);
    }

    private String periodEmoji(Milestone.Period period) {
        return period == Milestone.Period.MONTH ? "🗓️" : "📆";
    }

    private enum GoalStage {
        AWAITING_GOAL_DESCRIPTION
    }
}
