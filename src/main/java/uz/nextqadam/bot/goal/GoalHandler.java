package uz.nextqadam.bot.goal;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import uz.nextqadam.bot.common.telegram.TelegramExecutor;
import uz.nextqadam.bot.user.User;
import uz.nextqadam.bot.user.UserService;

@Component
public class GoalHandler {

    // TODO: bu holat xotirasi hozircha in-memory Map orqali saqlanmoqda (bir nechta instance/qayta ishga
    // tushirishda yo'qoladi) — keyinchalik Redis yoki DB (masalan alohida "conversation_state" jadvali) ga
    // ko'chirish kerak.
    private final Map<Long, GoalStage> stageByChatId = new ConcurrentHashMap<>();

    private final GoalService goalService;
    private final UserService userService;
    private final MilestoneRepository milestoneRepository;
    private final TelegramExecutor telegramExecutor;

    public GoalHandler(GoalService goalService, UserService userService, MilestoneRepository milestoneRepository,
                        TelegramExecutor telegramExecutor) {
        this.goalService = goalService;
        this.userService = userService;
        this.milestoneRepository = milestoneRepository;
        this.telegramExecutor = telegramExecutor;
    }

    public boolean isAwaitingGoalDescription(Long chatId) {
        return stageByChatId.get(chatId) == GoalStage.AWAITING_GOAL_DESCRIPTION;
    }

    public void handleNewGoalCommand(Update update) {
        Long chatId = update.getMessage().getChatId();
        stageByChatId.put(chatId, GoalStage.AWAITING_GOAL_DESCRIPTION);
        telegramExecutor.sendMessage(chatId,
                "Katta maqsadingizni bir necha jumla bilan yozing "
                        + "(masalan: \"6 oyda backend developer bo'lmoqchiman\")");
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
            telegramExecutor.sendMessage(chatId,
                    "🎯 Maqsadingiz saqlandi, lekin uni bosqichlarga bo'lishda xatolik yuz berdi. "
                            + "Birozdan so'ng /newgoal orqali qayta urinib ko'ring.");
            return;
        }

        telegramExecutor.sendMessage(chatId, formatGoalSummary(goal));

        goalService.getNextStep(user.getId())
                .ifPresent(task -> telegramExecutor.sendMessage(chatId, "📌 Bugungi NextQadam: " + task.getTitle()));
    }

    public void handleNextStepCommand(Update update) {
        Long chatId = update.getMessage().getChatId();
        User user = userService.findByTelegramId(chatId).orElse(null);
        if (user == null) {
            telegramExecutor.sendMessage(chatId, "Avval /start orqali ro'yxatdan o'ting.");
            return;
        }

        goalService.getNextStep(user.getId()).ifPresentOrElse(
                task -> telegramExecutor.sendMessage(chatId, "📌 Bugungi NextQadam: " + task.getTitle()),
                () -> telegramExecutor.sendMessage(chatId, "Sizda hali faol maqsad yo'q. /newgoal orqali birinchi maqsadingizni qo'shing.")
        );
    }

    private String formatGoalSummary(Goal goal) {
        List<Milestone> milestones = milestoneRepository.findAllByGoalId(goal.getId());

        StringBuilder sb = new StringBuilder();
        sb.append("🎯 <b>").append(goal.getTitle()).append("</b>\n\n");
        for (int i = 0; i < milestones.size(); i++) {
            sb.append(numberEmoji(i + 1)).append(" ").append(milestones.get(i).getTitle()).append("\n");
        }
        return sb.toString();
    }

    private String numberEmoji(int number) {
        if (number == 10) {
            return "🔟";
        }
        if (number >= 1 && number <= 9) {
            return number + "️⃣";
        }
        return number + ".";
    }

    private enum GoalStage {
        AWAITING_GOAL_DESCRIPTION
    }
}