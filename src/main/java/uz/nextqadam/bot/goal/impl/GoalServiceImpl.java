package uz.nextqadam.bot.goal.impl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import uz.nextqadam.bot.ai.AiClient;
import uz.nextqadam.bot.ai.AiClientException;
import uz.nextqadam.bot.ai.AiResponseParseException;
import uz.nextqadam.bot.ai.AiResponseParser;
import uz.nextqadam.bot.ai.PromptBuilder;
import uz.nextqadam.bot.ai.dto.GoalDecompositionResult;
import uz.nextqadam.bot.ai.dto.MilestoneDraft;
import uz.nextqadam.bot.ai.dto.TaskDraft;
import uz.nextqadam.bot.common.exception.NextQadamException;
import uz.nextqadam.bot.goal.Goal;
import uz.nextqadam.bot.goal.GoalRepository;
import uz.nextqadam.bot.goal.GoalService;
import uz.nextqadam.bot.goal.Milestone;
import uz.nextqadam.bot.goal.MilestoneRepository;
import uz.nextqadam.bot.goal.Task;
import uz.nextqadam.bot.goal.TaskRepository;
import uz.nextqadam.bot.memory.MemoryService;
import uz.nextqadam.bot.user.User;
import uz.nextqadam.bot.user.UserRepository;

@Service
public class GoalServiceImpl implements GoalService {

    private static final Logger log = LoggerFactory.getLogger(GoalServiceImpl.class);

    private static final int TITLE_MAX_LENGTH = 80;
    private static final int MONTH_PERIOD_DAYS = 30;
    private static final int WEEK_PERIOD_DAYS = 7;

    private final GoalRepository goalRepository;
    private final MilestoneRepository milestoneRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final AiClient aiClient;
    private final PromptBuilder promptBuilder;
    private final AiResponseParser aiResponseParser;
    private final MemoryService memoryService;

    public GoalServiceImpl(GoalRepository goalRepository, MilestoneRepository milestoneRepository,
                            TaskRepository taskRepository, UserRepository userRepository,
                            AiClient aiClient, PromptBuilder promptBuilder, AiResponseParser aiResponseParser,
                            MemoryService memoryService) {
        this.goalRepository = goalRepository;
        this.milestoneRepository = milestoneRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.aiClient = aiClient;
        this.promptBuilder = promptBuilder;
        this.aiResponseParser = aiResponseParser;
        this.memoryService = memoryService;
    }

    @Override
    public Goal createGoalWithAiDecomposition(UUID userId, String rawDescription) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NextQadamException("Foydalanuvchi topilmadi: " + userId));

        Goal goal = Goal.builder()
                .user(user)
                .title(truncate(rawDescription, TITLE_MAX_LENGTH))
                .description(rawDescription)
                .status(Goal.Status.ACTIVE)
                .build();
        goal = goalRepository.save(goal);

        try {
            String memoryContext = memoryService.buildContextBlock(userId);
            String systemPrompt = promptBuilder.buildGoalDecompositionPrompt(rawDescription, memoryContext, user.getLanguage());
            String rawJson = aiClient.complete(systemPrompt, rawDescription);
            GoalDecompositionResult result = aiResponseParser.parseGoalDecomposition(rawJson);

            goal.setTitle(truncate(result.title(), TITLE_MAX_LENGTH));
            goal = goalRepository.save(goal);

            for (MilestoneDraft milestoneDraft : result.milestones()) {
                createMilestoneWithTasks(goal, milestoneDraft);
            }

            memoryService.remember(userId, "so'nggi_maqsad", goal.getTitle(), 5);
        } catch (AiClientException | AiResponseParseException e) {
            log.error("Maqsadni AI orqali bosqichlarga bo'lishda xatolik yuz berdi. goalId={}", goal.getId(), e);
            boolean transientFailure = e instanceof AiClientException aiClientException
                    && aiClientException.isTransientFailure();
            String marker = transientFailure ? AI_DECOMPOSITION_TRANSIENT_FAILURE_MARKER : AI_DECOMPOSITION_FAILURE_MARKER;
            goal.setDescription(rawDescription + "\n\n" + marker);
            goal = goalRepository.save(goal);
        }

        return goal;
    }

    @Override
    public Goal getOrCreateDailyCatchAllGoal(UUID userId) {
        return goalRepository.findByUserIdAndTitle(userId, DAILY_CATCH_ALL_GOAL_TITLE)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new NextQadamException("Foydalanuvchi topilmadi: " + userId));
                    Goal catchAllGoal = Goal.builder()
                            .user(user)
                            .title(DAILY_CATCH_ALL_GOAL_TITLE)
                            .description("Brain Dump orqali qo'shilgan, aniq maqsadga bog'lanmagan vazifalar uchun.")
                            .status(Goal.Status.ACTIVE)
                            .build();
                    return goalRepository.save(catchAllGoal);
                });
    }

    private void createMilestoneWithTasks(Goal goal, MilestoneDraft milestoneDraft) {
        Milestone.Period period = Milestone.Period.valueOf(milestoneDraft.period());

        Milestone milestone = Milestone.builder()
                .goal(goal)
                .title(milestoneDraft.title())
                .period(period)
                .status(Milestone.Status.PENDING)
                .build();
        milestone = milestoneRepository.save(milestone);

        int periodDays = period == Milestone.Period.MONTH ? MONTH_PERIOD_DAYS : WEEK_PERIOD_DAYS;
        List<TaskDraft> taskDrafts = milestoneDraft.tasks();
        Instant now = Instant.now();

        for (int i = 0; i < taskDrafts.size(); i++) {
            TaskDraft taskDraft = taskDrafts.get(i);
            long dayOffset = Math.max(1, Math.round((double) (i + 1) * periodDays / taskDrafts.size()));

            Task task = Task.builder()
                    .goal(goal)
                    .milestone(milestone)
                    .title(taskDraft.title())
                    .dueDate(now.plus(dayOffset, ChronoUnit.DAYS))
                    .status(Task.Status.PENDING)
                    .estimatedMinutes(taskDraft.estimatedMinutes())
                    .build();
            taskRepository.save(task);
        }
    }

    @Override
    public Optional<Task> getNextStep(UUID userId) {
        List<Task> tasks = taskRepository.findByGoal_User_IdAndStatusOrderByIsTodayPriorityDescDueDateAsc(
                userId, Task.Status.PENDING);
        return tasks.stream().findFirst();
    }

    @Override
    public Optional<Task> getNextStepForGoal(UUID goalId) {
        return taskRepository.findFirstByGoal_IdAndStatusOrderByIsTodayPriorityDescDueDateAsc(goalId, Task.Status.PENDING);
    }

    @Override
    public Optional<Task> getCurrentTaskForUser(UUID userId) {
        return getNextStep(userId);
    }

    @Override
    public Task markTaskDone(UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NextQadamException("Task topilmadi: " + taskId));
        task.setStatus(Task.Status.DONE);
        // Bajarilgan vazifa — ketma-ket kechiktirish "streak"ining uzilishi hisoblanadi.
        task.setConsecutiveSnoozeCount(0);
        return taskRepository.save(task);
    }

    @Override
    public List<Goal> getActiveGoals(UUID userId) {
        return goalRepository.findAllByUserIdAndStatus(userId, Goal.Status.ACTIVE);
    }

    @Override
    public Map<Goal, ProgressStats> getGoalsWithProgress(UUID userId) {
        Map<Goal, ProgressStats> goalsWithProgress = new LinkedHashMap<>();
        for (Goal goal : getActiveGoals(userId)) {
            List<Task> tasks = taskRepository.findAllByGoalId(goal.getId());
            int totalCount = tasks.size();
            int doneCount = (int) tasks.stream().filter(task -> task.getStatus() == Task.Status.DONE).count();
            int percentComplete = totalCount == 0 ? 0 : (int) Math.round(doneCount * 100.0 / totalCount);
            goalsWithProgress.put(goal, new ProgressStats(doneCount, totalCount, percentComplete));
        }
        return goalsWithProgress;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}