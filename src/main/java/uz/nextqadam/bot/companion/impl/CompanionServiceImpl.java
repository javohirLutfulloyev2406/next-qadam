package uz.nextqadam.bot.companion.impl;

import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import uz.nextqadam.bot.ai.AiClient;
import uz.nextqadam.bot.ai.AiClientException;
import uz.nextqadam.bot.ai.PromptBuilder;
import uz.nextqadam.bot.common.LocalizationService;
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

    private final MemoryService memoryService;
    private final GoalService goalService;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final AiClient aiClient;
    private final PromptBuilder promptBuilder;
    private final LocalizationService localizationService;

    public CompanionServiceImpl(MemoryService memoryService, GoalService goalService, TaskRepository taskRepository,
                                 UserRepository userRepository, AiClient aiClient, PromptBuilder promptBuilder,
                                 LocalizationService localizationService) {
        this.memoryService = memoryService;
        this.goalService = goalService;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.aiClient = aiClient;
        this.promptBuilder = promptBuilder;
        this.localizationService = localizationService;
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
            return localizationService.get(user.getLanguage(),
                    "companion.freechat.fallback." + user.getTonePreference().name());
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
            return localizationService.get(user.getLanguage(),
                    "companion.motivation.fallback." + user.getTonePreference().name());
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
            return localizationService.get(user.getLanguage(), "companion.sos.fallback");
        }
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NextQadamException("Foydalanuvchi topilmadi: " + userId));
    }
}
