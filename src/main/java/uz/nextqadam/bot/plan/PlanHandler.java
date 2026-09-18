package uz.nextqadam.bot.plan;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import uz.nextqadam.bot.ai.AiClientException;
import uz.nextqadam.bot.ai.AiResponseParseException;
import uz.nextqadam.bot.common.keyboard.KeyboardService;
import uz.nextqadam.bot.common.keyboard.KeyboardService.CheckinTaskOption;
import uz.nextqadam.bot.common.telegram.TelegramExecutor;
import uz.nextqadam.bot.goal.Task;
import uz.nextqadam.bot.goal.TaskRepository;
import uz.nextqadam.bot.plan.PlanService.BrainDumpSummary;
import uz.nextqadam.bot.user.User;
import uz.nextqadam.bot.user.UserService;

@Component
public class PlanHandler {

    private static final String CHECKIN_TOGGLE_PREFIX = "CHECKIN_TOGGLE_";
    private static final String CHECKIN_CONFIRM_CALLBACK = "CHECKIN_CONFIRM";
    private static final int PLAN_DAY_TASK_LIMIT = 15;
    private static final int MAX_TODAY_PRIORITIES = 3;
    private static final int IDEAS_LIST_LIMIT = 10;

    // TODO: xuddi boshqa handler'lardagi kabi — in-memory xotira, instance qayta ishga tushirilganda
    // yo'qoladi, keyinchalik Redis/DB'ga ko'chirish kerak.
    private final Map<Long, Set<UUID>> selectedTaskIdsByChatId = new ConcurrentHashMap<>();
    private final Map<Long, PlanStage> stageByChatId = new ConcurrentHashMap<>();

    private final PlanService planService;
    private final UserService userService;
    private final TaskRepository taskRepository;
    private final IdeaRepository ideaRepository;
    private final KeyboardService keyboardService;
    private final TelegramExecutor telegramExecutor;

    public PlanHandler(PlanService planService, UserService userService, TaskRepository taskRepository,
                        IdeaRepository ideaRepository, KeyboardService keyboardService,
                        TelegramExecutor telegramExecutor) {
        this.planService = planService;
        this.userService = userService;
        this.taskRepository = taskRepository;
        this.ideaRepository = ideaRepository;
        this.keyboardService = keyboardService;
        this.telegramExecutor = telegramExecutor;
    }

    public boolean isAwaitingBrainDumpText(Long chatId) {
        return stageByChatId.get(chatId) == PlanStage.AWAITING_BRAINDUMP_TEXT;
    }

    public boolean isCheckinToggleCallback(String callbackData) {
        return callbackData != null && callbackData.startsWith(CHECKIN_TOGGLE_PREFIX);
    }

    public boolean isCheckinConfirmCallback(String callbackData) {
        return CHECKIN_CONFIRM_CALLBACK.equals(callbackData);
    }

    public void handlePlanDayCommand(Update update) {
        Long chatId = update.getMessage().getChatId();
        User user = userService.findByTelegramId(chatId).orElse(null);
        if (user == null) {
            telegramExecutor.sendMessage(chatId, "Avval /start orqali ro'yxatdan o'ting.");
            return;
        }

        List<Task> candidateTasks = pendingTasksForPlanning(user.getId());
        if (candidateTasks.isEmpty()) {
            telegramExecutor.sendMessage(chatId, "Hozircha rejalashtirish kerak bo'lgan vazifangiz yo'q.");
            return;
        }

        selectedTaskIdsByChatId.put(chatId, new LinkedHashSet<>());
        telegramExecutor.sendMessageWithKeyboard(chatId,
                "🌅 <b>Bugun nimalarga ustuvorlik berasiz?</b>\n\nEng muhim 1-3 tasini tanlang:",
                buildCheckinKeyboard(candidateTasks, chatId));
    }

    public void handleCheckinToggle(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        UUID taskId = UUID.fromString(callbackQuery.getData().substring(CHECKIN_TOGGLE_PREFIX.length()));

        Set<UUID> selected = selectedTaskIdsByChatId.computeIfAbsent(chatId, id -> new LinkedHashSet<>());
        if (selected.contains(taskId)) {
            selected.remove(taskId);
        } else if (selected.size() >= MAX_TODAY_PRIORITIES) {
            telegramExecutor.answerCallbackQuery(callbackQuery.getId(), "Ko'pi bilan 3 ta tanlash mumkin", true);
            return;
        } else {
            selected.add(taskId);
        }

        User user = userService.findByTelegramId(chatId).orElse(null);
        if (user == null) {
            return;
        }
        List<Task> candidateTasks = pendingTasksForPlanning(user.getId());
        telegramExecutor.editMessageReplyMarkup(chatId, messageId, buildCheckinKeyboard(candidateTasks, chatId));
    }

    public void handleCheckinConfirm(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        User user = userService.findByTelegramId(chatId).orElse(null);
        if (user == null) {
            return;
        }

        List<UUID> taskIds = new ArrayList<>(selectedTaskIdsByChatId.getOrDefault(chatId, Set.of()));
        planService.setTodayPriorities(user.getId(), taskIds);
        selectedTaskIdsByChatId.remove(chatId);

        String taskList = taskIds.stream()
                .map(taskRepository::findById)
                .flatMap(Optional::stream)
                .map(task -> "• " + task.getTitle())
                .collect(Collectors.joining("\n"));

        telegramExecutor.editMessageText(chatId, messageId,
                "✅ <b>Bugungi ustuvorlik belgilandi:</b>\n\n" + taskList + "\n\nOmad! 🚀");
        telegramExecutor.editMessageReplyMarkup(chatId, messageId,
                InlineKeyboardMarkup.builder().keyboard(List.of()).build());
    }

    public void handleBrainDumpCommand(Update update) {
        Long chatId = update.getMessage().getChatId();
        stageByChatId.put(chatId, PlanStage.AWAITING_BRAINDUMP_TEXT);
        telegramExecutor.sendMessage(chatId,
                "🧠 Xayolingizda nima bor? Bir nechta narsani bir paytda yozib tashlashingiz mumkin — "
                        + "men ularni tartiblab beraman.");
    }

    public void handleBrainDumpText(Update update) {
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        String rawText = message.getText().trim();
        stageByChatId.remove(chatId);

        User user = userService.findByTelegramId(chatId).orElse(null);
        if (user == null) {
            telegramExecutor.sendMessage(chatId, "Avval /start orqali ro'yxatdan o'ting.");
            return;
        }

        BrainDumpSummary summary;
        try {
            summary = planService.processBrainDump(user.getId(), rawText);
        } catch (AiClientException | AiResponseParseException e) {
            telegramExecutor.sendMessage(chatId,
                    "Kechirasiz, fikrlaringizni tartiblashda xatolik yuz berdi. Birozdan so'ng qayta urinib ko'ring.");
            return;
        }

        telegramExecutor.sendMessageWithReplyKeyboard(chatId, formatBrainDumpSummary(summary),
                keyboardService.buildMainMenuKeyboard());
    }

    public void handleIdeasCommand(Update update) {
        Long chatId = update.getMessage().getChatId();
        User user = userService.findByTelegramId(chatId).orElse(null);
        if (user == null) {
            telegramExecutor.sendMessage(chatId, "Avval /start orqali ro'yxatdan o'ting.");
            return;
        }

        List<Idea> ideas = ideaRepository.findByUser_IdOrderByCreatedAtDesc(user.getId());
        if (ideas.isEmpty()) {
            telegramExecutor.sendMessage(chatId,
                    "Hali hech qanday g'oya saqlanmagan. /braindump orqali fikrlaringizni yozib tashlang.");
            return;
        }

        List<Idea> topIdeas = ideas.size() > IDEAS_LIST_LIMIT ? ideas.subList(0, IDEAS_LIST_LIMIT) : ideas;
        StringBuilder sb = new StringBuilder("💡 <b>G'oyalaringiz:</b>\n\n");
        for (Idea idea : topIdeas) {
            sb.append("• ").append(idea.getContent()).append("\n");
        }
        telegramExecutor.sendMessage(chatId, sb.toString());
    }

    private String formatBrainDumpSummary(BrainDumpSummary summary) {
        StringBuilder sb = new StringBuilder("🧠 <b>Tartibga solindi!</b>\n\n");
        if (summary.taskCount() > 0) {
            sb.append("✅ ").append(summary.taskCount()).append(" ta vazifa qo'shildi\n");
        }
        if (summary.ideaCount() > 0) {
            sb.append("💡 ").append(summary.ideaCount()).append(" ta g'oya saqlandi\n");
        }
        if (summary.reminderCount() > 0) {
            sb.append("⏰ ").append(summary.reminderCount()).append(" ta eslatma o'rnatildi\n");
        }
        sb.append("\n/goals orqali vazifalarni, /ideas orqali g'oyalarni ko'rishingiz mumkin.");
        return sb.toString();
    }

    private List<Task> pendingTasksForPlanning(UUID userId) {
        return taskRepository.findByGoal_User_IdAndStatusOrderByDueDateAsc(userId, Task.Status.PENDING,
                PageRequest.of(0, PLAN_DAY_TASK_LIMIT));
    }

    private InlineKeyboardMarkup buildCheckinKeyboard(List<Task> candidateTasks, Long chatId) {
        Set<UUID> selected = selectedTaskIdsByChatId.getOrDefault(chatId, Set.of());
        List<CheckinTaskOption> options = candidateTasks.stream()
                .map(task -> new CheckinTaskOption(task.getId(), task.getTitle(), selected.contains(task.getId())))
                .toList();
        return keyboardService.buildMorningCheckinKeyboard(options);
    }

    private enum PlanStage {
        AWAITING_BRAINDUMP_TEXT
    }
}
