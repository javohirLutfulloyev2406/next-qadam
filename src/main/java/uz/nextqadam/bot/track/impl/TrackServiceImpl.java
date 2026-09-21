package uz.nextqadam.bot.track.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import uz.nextqadam.bot.ai.AiClient;
import uz.nextqadam.bot.ai.AiClientException;
import uz.nextqadam.bot.ai.AiResponseParseException;
import uz.nextqadam.bot.ai.AiResponseParser;
import uz.nextqadam.bot.ai.PromptBuilder;
import uz.nextqadam.bot.ai.dto.EveningCheckinResult;
import uz.nextqadam.bot.ai.dto.GoalDriftResult;
import uz.nextqadam.bot.ai.dto.TaskClassification;
import uz.nextqadam.bot.ai.dto.TaskSummaryForPrompt;
import uz.nextqadam.bot.ai.dto.WeeklyRetrospective;
import uz.nextqadam.bot.common.util.TimeUtil;
import uz.nextqadam.bot.goal.Goal;
import uz.nextqadam.bot.goal.GoalRepository;
import uz.nextqadam.bot.goal.GoalService;
import uz.nextqadam.bot.goal.Task;
import uz.nextqadam.bot.goal.TaskRepository;
import uz.nextqadam.bot.common.enums.Language;
import uz.nextqadam.bot.common.exception.NextQadamException;
import uz.nextqadam.bot.track.CheckIn;
import uz.nextqadam.bot.track.CheckInRepository;
import uz.nextqadam.bot.track.TrackService;
import uz.nextqadam.bot.user.User;
import uz.nextqadam.bot.user.UserRepository;

@Service
public class TrackServiceImpl implements TrackService {

    private static final Logger log = LoggerFactory.getLogger(TrackServiceImpl.class);

    private static final String AI_PARSE_FAILED_MARKER = "[AI_PARSE_FAILED]";
    private static final int WEEKLY_WINDOW_DAYS = 7;
    private static final int DRIFT_WINDOW_DAYS = 14;
    private static final List<Task.Status> CHECKIN_STATUSES = List.of(Task.Status.PENDING, Task.Status.DONE);

    private final CheckInRepository checkInRepository;
    private final TaskRepository taskRepository;
    private final GoalRepository goalRepository;
    private final GoalService goalService;
    private final UserRepository userRepository;
    private final PromptBuilder promptBuilder;
    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;

    public TrackServiceImpl(CheckInRepository checkInRepository, TaskRepository taskRepository,
                             GoalRepository goalRepository, GoalService goalService, UserRepository userRepository,
                             PromptBuilder promptBuilder, AiClient aiClient, AiResponseParser aiResponseParser) {
        this.checkInRepository = checkInRepository;
        this.taskRepository = taskRepository;
        this.goalRepository = goalRepository;
        this.goalService = goalService;
        this.userRepository = userRepository;
        this.promptBuilder = promptBuilder;
        this.aiClient = aiClient;
        this.aiResponseParser = aiResponseParser;
    }

    @Override
    public EveningCheckinResult processEveningCheckin(UUID userId, String rawText) {
        List<Task> todaysTasks = findTodaysTasksForCheckin(userId);

        if (todaysTasks.isEmpty()) {
            saveEveningCheckIn(userId, rawText, rawText);
            return new EveningCheckinResult(List.of(), null);
        }

        Map<UUID, Task> taskById = todaysTasks.stream().collect(Collectors.toMap(Task::getId, task -> task));
        List<TaskSummaryForPrompt> summaries = todaysTasks.stream()
                .map(task -> new TaskSummaryForPrompt(task.getId(), task.getTitle()))
                .toList();

        Language language = languageOf(userId);
        EveningCheckinResult result;
        try {
            String prompt = promptBuilder.buildEveningCheckinPrompt(rawText, summaries, language);
            String rawJson = aiClient.complete(prompt, rawText);
            result = aiResponseParser.parseEveningCheckin(rawJson);
        } catch (AiClientException | AiResponseParseException e) {
            log.error("Kechki check-inni AI orqali tahlil qilishda xatolik. userId={}", userId, e);
            saveEveningCheckIn(userId, rawText, AI_PARSE_FAILED_MARKER);
            return null;
        }

        for (TaskClassification classification : result.classifications()) {
            Task task = taskById.get(classification.taskId());
            if (task != null && classification.done() && task.getStatus() != Task.Status.DONE) {
                goalService.markTaskDone(task.getId());
            }
        }

        saveEveningCheckIn(userId, rawText, buildParsedSummary(result, taskById));
        return result;
    }

    @Override
    public WeeklyRetrospective generateWeeklyRetrospective(UUID userId) {
        List<Task> weekTasks = taskRepository.findByGoal_User_IdAndDueDateBetween(userId, weeklyWindowStart(),
                Instant.now());
        int total = weekTasks.size();
        List<String> completedTitles = weekTasks.stream()
                .filter(task -> task.getStatus() == Task.Status.DONE)
                .map(Task::getTitle)
                .toList();
        List<String> missedTitles = weekTasks.stream()
                .filter(task -> task.getStatus() != Task.Status.DONE)
                .map(Task::getTitle)
                .toList();
        int done = completedTitles.size();

        try {
            String prompt = promptBuilder.buildWeeklyRetrospectivePrompt(total, done, completedTitles, missedTitles,
                    languageOf(userId));
            String rawJson = aiClient.complete(prompt, "Haftalik xulosa yoz.");
            return aiResponseParser.parseWeeklyRetrospective(rawJson);
        } catch (AiClientException | AiResponseParseException e) {
            log.error("Haftalik retrospektivani AI orqali generatsiya qilishda xatolik. userId={}", userId, e);
            return new WeeklyRetrospective(
                    "Bu hafta " + done + "/" + total + " vazifa bajardingiz.",
                    "Davom eting!");
        }
    }

    @Override
    public GoalDriftResult checkGoalDrift(UUID userId) {
        Goal activeGoal = goalRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, Goal.Status.ACTIVE)
                .orElse(null);
        if (activeGoal == null) {
            return new GoalDriftResult(100, "Faol maqsad yo'q");
        }

        List<String> recentTaskTitles = taskRepository
                .findByGoal_User_IdAndDueDateBetween(userId, driftWindowStart(), Instant.now())
                .stream()
                .map(Task::getTitle)
                .toList();

        try {
            String prompt = promptBuilder.buildGoalDriftPrompt(activeGoal.getTitle(), recentTaskTitles, languageOf(userId));
            String rawJson = aiClient.complete(prompt, "Moslikni bahola.");
            return aiResponseParser.parseGoalDrift(rawJson);
        } catch (AiClientException | AiResponseParseException e) {
            log.error("Goal drift tekshiruvida xatolik. userId={}", userId, e);
            return new GoalDriftResult(100, "Tekshirishda xatolik yuz berdi, hozircha yo'nalish yaxshi deb "
                    + "hisoblaymiz.");
        }
    }

    @Override
    public WeeklyStats getWeeklyStats(UUID userId) {
        List<Task> weekTasks = taskRepository.findByGoal_User_IdAndDueDateBetween(userId, weeklyWindowStart(),
                Instant.now());
        int total = weekTasks.size();
        int done = (int) weekTasks.stream().filter(task -> task.getStatus() == Task.Status.DONE).count();
        return new WeeklyStats(total, done);
    }

    private List<Task> findTodaysTasksForCheckin(UUID userId) {
        Instant dayStart = TimeUtil.todayInTashkent().atStartOfDay(TimeUtil.TASHKENT_ZONE).toInstant();
        Instant dayEnd = dayStart.plus(1, ChronoUnit.DAYS);
        return taskRepository.findTodaysTasksForCheckin(userId, CHECKIN_STATUSES, dayStart, dayEnd);
    }

    private String buildParsedSummary(EveningCheckinResult result, Map<UUID, Task> taskById) {
        StringBuilder sb = new StringBuilder();
        for (TaskClassification classification : result.classifications()) {
            Task task = taskById.get(classification.taskId());
            String title = task != null ? task.getTitle() : "?";
            if (classification.done()) {
                sb.append("✅ ").append(title).append("\n");
            } else {
                sb.append("❌ ").append(title).append(" — ").append(classification.reason()).append("\n");
            }
        }
        return sb.toString().stripTrailing();
    }

    private void saveEveningCheckIn(UUID userId, String rawText, String parsedSummary) {
        LocalDate today = TimeUtil.todayInTashkent();
        CheckIn checkIn = checkInRepository.findByUserIdAndDateAndType(userId, today, CheckIn.Type.EVENING)
                .orElseGet(() -> CheckIn.builder()
                        .user(userRepository.getReferenceById(userId))
                        .date(today)
                        .type(CheckIn.Type.EVENING)
                        .build());
        checkIn.setRawText(rawText);
        checkIn.setParsedSummary(parsedSummary);
        checkInRepository.save(checkIn);
    }

    private Instant weeklyWindowStart() {
        return Instant.now().minus(WEEKLY_WINDOW_DAYS, ChronoUnit.DAYS);
    }

    private Instant driftWindowStart() {
        return Instant.now().minus(DRIFT_WINDOW_DAYS, ChronoUnit.DAYS);
    }

    private Language languageOf(UUID userId) {
        return userRepository.findById(userId)
                .map(User::getLanguage)
                .orElseThrow(() -> new NextQadamException("Foydalanuvchi topilmadi: " + userId));
    }
}
