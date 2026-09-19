package uz.nextqadam.bot.nudge.impl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import uz.nextqadam.bot.common.exception.NextQadamException;
import uz.nextqadam.bot.goal.Task;
import uz.nextqadam.bot.goal.TaskRepository;
import uz.nextqadam.bot.nudge.NudgeService;
import uz.nextqadam.bot.nudge.SnoozeResult;

@Service
public class NudgeServiceImpl implements NudgeService {

    private static final int SNOOZE_HOURS = 1;
    private static final int ADAPTIVE_SHRINK_THRESHOLD = 3;
    private static final int ADAPTIVE_SHRINK_DIVISOR = 3;
    private static final int MIN_ESTIMATED_MINUTES = 5;

    private final TaskRepository taskRepository;

    public NudgeServiceImpl(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public SnoozeResult snoozeTask(UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NextQadamException("Task topilmadi: " + taskId));

        task.setStatus(Task.Status.SNOOZED);
        task.setConsecutiveSnoozeCount(task.getConsecutiveSnoozeCount() + 1);
        task.setSnoozedUntil(Instant.now().plus(SNOOZE_HOURS, ChronoUnit.HOURS));

        if (task.getConsecutiveSnoozeCount() >= ADAPTIVE_SHRINK_THRESHOLD) {
            int currentEstimate = task.getEstimatedMinutes() != null ? task.getEstimatedMinutes() : 0;
            int newEstimate = Math.max(MIN_ESTIMATED_MINUTES, currentEstimate / ADAPTIVE_SHRINK_DIVISOR);

            task.setEstimatedMinutes(newEstimate);
            task.setConsecutiveSnoozeCount(0);
            task.setStatus(Task.Status.PENDING);
            task.setSnoozedUntil(null);

            Task saved = taskRepository.save(task);
            return new SnoozeResult(saved, true, newEstimate);
        }

        Task saved = taskRepository.save(task);
        int estimate = saved.getEstimatedMinutes() != null ? saved.getEstimatedMinutes() : 0;
        return new SnoozeResult(saved, false, estimate);
    }

    @Override
    public void requeueDueSnoozedTasks() {
        List<Task> dueTasks = taskRepository.findAllByStatusAndSnoozedUntilBefore(Task.Status.SNOOZED, Instant.now());
        for (Task task : dueTasks) {
            task.setStatus(Task.Status.PENDING);
            task.setSnoozedUntil(null);
            taskRepository.save(task);
        }
    }
}
