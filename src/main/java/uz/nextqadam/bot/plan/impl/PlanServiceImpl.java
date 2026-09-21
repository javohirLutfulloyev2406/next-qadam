package uz.nextqadam.bot.plan.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import uz.nextqadam.bot.ai.AiClient;
import uz.nextqadam.bot.ai.AiResponseParser;
import uz.nextqadam.bot.ai.PromptBuilder;
import uz.nextqadam.bot.ai.dto.BrainDumpResult;
import uz.nextqadam.bot.ai.dto.ReminderDraft;
import uz.nextqadam.bot.common.exception.NextQadamException;
import uz.nextqadam.bot.common.util.TimeUtil;
import uz.nextqadam.bot.goal.Goal;
import uz.nextqadam.bot.goal.GoalService;
import uz.nextqadam.bot.goal.Task;
import uz.nextqadam.bot.goal.TaskRepository;
import uz.nextqadam.bot.nudge.Reminder;
import uz.nextqadam.bot.nudge.ReminderRepository;
import uz.nextqadam.bot.plan.Idea;
import uz.nextqadam.bot.plan.IdeaRepository;
import uz.nextqadam.bot.plan.PlanService;
import uz.nextqadam.bot.track.CheckIn;
import uz.nextqadam.bot.track.CheckInRepository;
import uz.nextqadam.bot.user.User;
import uz.nextqadam.bot.user.UserRepository;

@Service
public class PlanServiceImpl implements PlanService {

    private static final int MAX_TODAY_PRIORITIES = 3;
    private static final String EVENING_HINT_KEYWORD = "kechqurun";
    private static final LocalTime EVENING_REMINDER_TIME = LocalTime.of(19, 0);
    private static final LocalTime MORNING_REMINDER_TIME = LocalTime.of(9, 0);
    private static final String CHECKIN_RAW_TEXT = "tugma orqali tanlandi";

    private final PromptBuilder promptBuilder;
    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;
    private final GoalService goalService;
    private final TaskRepository taskRepository;
    private final IdeaRepository ideaRepository;
    private final ReminderRepository reminderRepository;
    private final CheckInRepository checkInRepository;
    private final UserRepository userRepository;

    public PlanServiceImpl(PromptBuilder promptBuilder, AiClient aiClient, AiResponseParser aiResponseParser,
                            GoalService goalService, TaskRepository taskRepository, IdeaRepository ideaRepository,
                            ReminderRepository reminderRepository, CheckInRepository checkInRepository,
                            UserRepository userRepository) {
        this.promptBuilder = promptBuilder;
        this.aiClient = aiClient;
        this.aiResponseParser = aiResponseParser;
        this.goalService = goalService;
        this.taskRepository = taskRepository;
        this.ideaRepository = ideaRepository;
        this.reminderRepository = reminderRepository;
        this.checkInRepository = checkInRepository;
        this.userRepository = userRepository;
    }

    @Override
    public BrainDumpSummary processBrainDump(UUID userId, String rawText) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NextQadamException("Foydalanuvchi topilmadi: " + userId));

        String systemPrompt = promptBuilder.buildBrainDumpPrompt(rawText, user.getLanguage());
        String rawJson = aiClient.complete(systemPrompt, rawText);
        BrainDumpResult result = aiResponseParser.parseBrainDump(rawJson);

        int taskCount = saveTasks(userId, result.tasks());
        int ideaCount = saveIdeas(user, result.ideas());
        int reminderCount = saveReminders(user, result.reminders());

        return new BrainDumpSummary(taskCount, ideaCount, reminderCount);
    }

    @Override
    public List<Task> getTodayPriorityTasks(UUID userId) {
        return taskRepository.findByGoal_User_IdAndStatusOrderByIsTodayPriorityDescDueDateAsc(userId, Task.Status.PENDING)
                .stream()
                .filter(Task::isTodayPriority)
                .toList();
    }

    @Override
    public void setTodayPriorities(UUID userId, List<UUID> taskIds) {
        for (Task task : taskRepository.findAllByGoal_User_Id(userId)) {
            if (task.isTodayPriority()) {
                task.setTodayPriority(false);
                taskRepository.save(task);
            }
        }

        List<UUID> limitedIds = taskIds.size() > MAX_TODAY_PRIORITIES ? taskIds.subList(0, MAX_TODAY_PRIORITIES) : taskIds;
        List<Task> selectedTasks = new ArrayList<>();
        for (UUID taskId : limitedIds) {
            taskRepository.findById(taskId).ifPresent(task -> {
                task.setTodayPriority(true);
                taskRepository.save(task);
                selectedTasks.add(task);
            });
        }

        recordMorningCheckIn(userId, selectedTasks);
    }

    private void recordMorningCheckIn(UUID userId, List<Task> selectedTasks) {
        LocalDate today = TimeUtil.todayInTashkent();
        String parsedSummary = selectedTasks.stream().map(Task::getTitle).collect(Collectors.joining(", "));

        CheckIn checkIn = checkInRepository.findByUserIdAndDateAndType(userId, today, CheckIn.Type.MORNING)
                .orElseGet(() -> CheckIn.builder()
                        .user(userRepository.getReferenceById(userId))
                        .date(today)
                        .type(CheckIn.Type.MORNING)
                        .build());
        checkIn.setRawText(CHECKIN_RAW_TEXT);
        checkIn.setParsedSummary(parsedSummary);
        checkInRepository.save(checkIn);
    }

    private int saveTasks(UUID userId, List<String> taskTitles) {
        if (taskTitles == null || taskTitles.isEmpty()) {
            return 0;
        }

        Goal catchAllGoal = goalService.getOrCreateDailyCatchAllGoal(userId);
        Instant tomorrow = Instant.now().plus(1, ChronoUnit.DAYS);

        for (String title : taskTitles) {
            Task task = Task.builder()
                    .goal(catchAllGoal)
                    .title(title)
                    .dueDate(tomorrow)
                    .status(Task.Status.PENDING)
                    .build();
            taskRepository.save(task);
        }
        return taskTitles.size();
    }

    private int saveIdeas(User user, List<String> ideaContents) {
        if (ideaContents == null || ideaContents.isEmpty()) {
            return 0;
        }

        for (String content : ideaContents) {
            Idea idea = Idea.builder().user(user).content(content).build();
            ideaRepository.save(idea);
        }
        return ideaContents.size();
    }

    private int saveReminders(User user, List<ReminderDraft> reminderDrafts) {
        if (reminderDrafts == null || reminderDrafts.isEmpty()) {
            return 0;
        }

        for (ReminderDraft draft : reminderDrafts) {
            Reminder reminder = Reminder.builder()
                    .user(user)
                    .task(null)
                    .content(draft.content())
                    .scheduledAt(resolveReminderTime(draft.whenHint()))
                    .tone(user.getTonePreference())
                    .status(Reminder.Status.PENDING)
                    .build();
            reminderRepository.save(reminder);
        }
        return reminderDrafts.size();
    }

    private Instant resolveReminderTime(String whenHint) {
        LocalDate tomorrow = TimeUtil.todayInTashkent().plusDays(1);
        boolean isEvening = whenHint != null && whenHint.toLowerCase().contains(EVENING_HINT_KEYWORD);
        LocalTime time = isEvening ? EVENING_REMINDER_TIME : MORNING_REMINDER_TIME;
        return tomorrow.atTime(time).atZone(TimeUtil.TASHKENT_ZONE).toInstant();
    }
}
