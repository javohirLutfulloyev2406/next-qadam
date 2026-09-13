package uz.nextqadam.bot.goal.impl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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

    public GoalServiceImpl(GoalRepository goalRepository, MilestoneRepository milestoneRepository,
                            TaskRepository taskRepository, UserRepository userRepository,
                            AiClient aiClient, PromptBuilder promptBuilder, AiResponseParser aiResponseParser) {
        this.goalRepository = goalRepository;
        this.milestoneRepository = milestoneRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.aiClient = aiClient;
        this.promptBuilder = promptBuilder;
        this.aiResponseParser = aiResponseParser;
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
            String systemPrompt = promptBuilder.buildGoalDecompositionPrompt(rawDescription);
            String rawJson = aiClient.complete(systemPrompt, rawDescription);
            GoalDecompositionResult result = aiResponseParser.parseGoalDecomposition(rawJson);

            goal.setTitle(truncate(result.title(), TITLE_MAX_LENGTH));
            goal = goalRepository.save(goal);

            for (MilestoneDraft milestoneDraft : result.milestones()) {
                createMilestoneWithTasks(goal, milestoneDraft);
            }
        } catch (AiClientException | AiResponseParseException e) {
            log.error("Maqsadni AI orqali bosqichlarga bo'lishda xatolik yuz berdi. goalId={}", goal.getId(), e);
            goal.setDescription(rawDescription + "\n\n" + AI_DECOMPOSITION_FAILURE_MARKER);
            goal = goalRepository.save(goal);
        }

        return goal;
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
        return taskRepository.findFirstByGoal_User_IdAndStatusOrderByDueDateAsc(userId, Task.Status.PENDING);
    }

    @Override
    public Task markTaskDone(UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NextQadamException("Task topilmadi: " + taskId));
        task.setStatus(Task.Status.DONE);
        return taskRepository.save(task);
    }

    @Override
    public List<Goal> getActiveGoals(UUID userId) {
        return goalRepository.findAllByUserIdAndStatus(userId, Goal.Status.ACTIVE);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}