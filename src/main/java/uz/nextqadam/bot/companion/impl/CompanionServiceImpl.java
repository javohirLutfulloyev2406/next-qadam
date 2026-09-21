package uz.nextqadam.bot.companion.impl;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import uz.nextqadam.bot.ai.AiClient;
import uz.nextqadam.bot.ai.AiClientException;
import uz.nextqadam.bot.ai.PromptBuilder;
import uz.nextqadam.bot.common.enums.ToneType;
import uz.nextqadam.bot.common.exception.NextQadamException;
import uz.nextqadam.bot.companion.CompanionService;
import uz.nextqadam.bot.goal.Goal;
import uz.nextqadam.bot.goal.GoalService;
import uz.nextqadam.bot.goal.Task;
import uz.nextqadam.bot.goal.TaskRepository;
import uz.nextqadam.bot.memory.MemoryService;
import uz.nextqadam.bot.user.User;
import uz.nextqadam.bot.user.UserRepository;

@Service
public class CompanionServiceImpl implements CompanionService {

    private static final Logger log = LoggerFactory.getLogger(CompanionServiceImpl.class);

    private static final Map<ToneType, String> FREE_CHAT_FALLBACK = Map.of(
            ToneType.SOFT, "Hozir sizni eshitolmayapman, biroz keyinroq qayta urinib ko'ring 🙂",
            ToneType.NORMAL, "Hozir javob berolmayapman, birozdan so'ng qayta urinib ko'ring.",
            ToneType.HARD, "Hozir ulanishda muammo bor. Birozdan so'ng qayta yozing.",
            ToneType.HARDCORE, "Hozir javob yo'q — signal muammosi. Birozdan so'ng qaytadan urin."
    );

    private static final Map<ToneType, String> MOTIVATION_FALLBACK = Map.of(
            ToneType.SOFT, "Har bir kichik qadam ham muhim. O'zingizga vaqt bering, siz to'g'ri yo'ldasiz 🌱",
            ToneType.NORMAL, "Davom eting — har bir bajarilgan vazifa sizni maqsadga yaqinlashtiradi.",
            ToneType.HARD, "Gapni cho'zmang — bugungi qadamni tashlang. Harakat natijani keltiradi.",
            ToneType.HARDCORE, "Bahona yo'q. Hozir turing va harakat qiling — kutish sizni hech qayerga olib bormaydi."
    );

    private static final String SOS_FALLBACK = "Shu vazifaning eng kichik qismini — atigi bir amalni — hozir qil.";

    private final MemoryService memoryService;
    private final GoalService goalService;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final AiClient aiClient;
    private final PromptBuilder promptBuilder;

    public CompanionServiceImpl(MemoryService memoryService, GoalService goalService, TaskRepository taskRepository,
                                 UserRepository userRepository, AiClient aiClient, PromptBuilder promptBuilder) {
        this.memoryService = memoryService;
        this.goalService = goalService;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.aiClient = aiClient;
        this.promptBuilder = promptBuilder;
    }

    @Override
    public String generateFreeChatReply(UUID userId, String userMessage) {
        User user = findUser(userId);
        String memoryContext = memoryService.buildContextBlock(userId);
        String recentGoalsSummary = goalService.getActiveGoals(userId).stream()
                .map(Goal::getTitle)
                .collect(Collectors.joining(", "));

        try {
            String systemPrompt = promptBuilder.buildFreeChatSystemPrompt(user.getTonePreference(), memoryContext,
                    recentGoalsSummary, user.getLanguage());
            return aiClient.completeText(systemPrompt, userMessage);
        } catch (AiClientException e) {
            log.warn("Erkin suhbat javobini olishda xatolik. userId={}", userId, e);
            return FREE_CHAT_FALLBACK.get(user.getTonePreference());
        }
    }

    @Override
    public String generateMotivation(UUID userId) {
        User user = findUser(userId);

        String lastCompletedTask = taskRepository
                .findFirstByGoal_User_IdAndStatusOrderByUpdatedAtDesc(userId, Task.Status.DONE)
                .map(Task::getTitle)
                .orElse("");
        String activeGoalTitle = goalService.getActiveGoals(userId).stream()
                .findFirst()
                .map(Goal::getTitle)
                .orElse("");
        int completedTaskCount = (int) taskRepository.countByGoal_User_IdAndStatus(userId, Task.Status.DONE);

        try {
            String systemPrompt = promptBuilder.buildMotivationPrompt(user.getTonePreference(), lastCompletedTask,
                    activeGoalTitle, completedTaskCount, user.getLanguage());
            return aiClient.completeText(systemPrompt, "Menga motivatsion xabar yoz.");
        } catch (AiClientException e) {
            log.warn("Motivatsion xabar olishda xatolik. userId={}", userId, e);
            return MOTIVATION_FALLBACK.get(user.getTonePreference());
        }
    }

    @Override
    public String generateSosMicroStep(UUID userId) {
        User user = findUser(userId);

        Task currentTask = goalService.getCurrentTaskForUser(userId).orElse(null);
        if (currentTask == null) {
            return null;
        }

        int estimatedMinutes = currentTask.getEstimatedMinutes() != null ? currentTask.getEstimatedMinutes() : 0;

        try {
            String systemPrompt = promptBuilder.buildSosPrompt(user.getTonePreference(), currentTask.getTitle(),
                    estimatedMinutes, user.getLanguage());
            return aiClient.completeText(systemPrompt, "Bu vazifani 5 daqiqalik mikro-qadamga qisqartir.");
        } catch (AiClientException e) {
            log.warn("SOS mikro-qadam olishda xatolik. userId={}", userId, e);
            return SOS_FALLBACK;
        }
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NextQadamException("Foydalanuvchi topilmadi: " + userId));
    }
}
