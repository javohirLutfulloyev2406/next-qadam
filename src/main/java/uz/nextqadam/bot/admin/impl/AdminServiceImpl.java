package uz.nextqadam.bot.admin.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import uz.nextqadam.bot.admin.AdminService;
import uz.nextqadam.bot.admin.SystemStats;
import uz.nextqadam.bot.common.errorlog.ErrorLogEntity;
import uz.nextqadam.bot.common.errorlog.ErrorLogRepository;
import uz.nextqadam.bot.common.telegram.TelegramExecutor;
import uz.nextqadam.bot.goal.Goal;
import uz.nextqadam.bot.goal.GoalRepository;
import uz.nextqadam.bot.goal.Task;
import uz.nextqadam.bot.goal.TaskRepository;
import uz.nextqadam.bot.track.CheckInRepository;
import uz.nextqadam.bot.user.User;
import uz.nextqadam.bot.user.UserRepository;
import uz.nextqadam.bot.user.UserRepository.UserSummaryProjection;

@Service
public class AdminServiceImpl implements AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

    private static final ZoneId ZONE = ZoneId.of("Asia/Tashkent");
    private static final int SEARCH_LIMIT = 10;
    // Telegram flood-limitiga tegib qolmaslik uchun har xabar orasida kutish.
    private static final long BROADCAST_DELAY_MS = 50;

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final CheckInRepository checkInRepository;
    private final GoalRepository goalRepository;
    private final ErrorLogRepository errorLogRepository;
    private final TelegramExecutor telegramExecutor;

    public AdminServiceImpl(UserRepository userRepository, TaskRepository taskRepository,
                             CheckInRepository checkInRepository, GoalRepository goalRepository,
                             ErrorLogRepository errorLogRepository, TelegramExecutor telegramExecutor) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.checkInRepository = checkInRepository;
        this.goalRepository = goalRepository;
        this.errorLogRepository = errorLogRepository;
        this.telegramExecutor = telegramExecutor;
    }

    @Override
    public SystemStats getSystemStats() {
        LocalDate today = LocalDate.now(ZONE);
        Instant dayStart = today.atStartOfDay(ZONE).toInstant();
        Instant dayEnd = dayStart.plus(1, ChronoUnit.DAYS);
        Instant last24h = Instant.now().minus(24, ChronoUnit.HOURS);

        int totalUsers = (int) userRepository.count();
        int newUsersToday = (int) userRepository.countByCreatedAtBetween(dayStart, dayEnd);

        Set<UUID> activeUserIds = new HashSet<>();
        activeUserIds.addAll(checkInRepository.findDistinctUserIdsByDate(today));
        activeUserIds.addAll(taskRepository.findDistinctUserIdsByStatusAndUpdatedAtBetween(
                Task.Status.DONE, dayStart, dayEnd));

        int totalActiveGoals = (int) goalRepository.countByStatus(Goal.Status.ACTIVE);
        int tasksDoneToday = (int) taskRepository.countByStatusAndUpdatedAtBetween(Task.Status.DONE, dayStart, dayEnd);
        int errorsLast24h = (int) errorLogRepository.countByCreatedAtGreaterThanEqual(last24h);

        return new SystemStats(totalUsers, newUsersToday, activeUserIds.size(), totalActiveGoals, tasksDoneToday,
                errorsLast24h);
    }

    @Override
    public BroadcastResult broadcastMessage(String message) {
        List<User> users = userRepository.findAll();
        int sentCount = 0;
        int failedCount = 0;

        for (User user : users) {
            try {
                if (telegramExecutor.sendMessageForBroadcast(user.getTelegramId(), message)) {
                    sentCount++;
                } else {
                    failedCount++;
                }
            } catch (Exception e) {
                failedCount++;
                log.warn("Broadcast xabarini yuborishda kutilmagan xatolik. telegramId={}", user.getTelegramId(), e);
            }
            sleepBetweenBroadcastMessages();
        }

        return new BroadcastResult(users.size(), sentCount, failedCount);
    }

    private void sleepBetweenBroadcastMessages() {
        try {
            Thread.sleep(BROADCAST_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public List<UserSummaryProjection> searchUsers(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return userRepository.searchUsers(query.trim(), PageRequest.of(0, SEARCH_LIMIT));
    }

    @Override
    public List<ErrorLogEntity> getRecentErrors(int limit) {
        return errorLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit));
    }
}
