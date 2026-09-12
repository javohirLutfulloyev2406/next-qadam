package uz.nextqadam.bot.goal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface GoalService {

    Goal createGoal(UUID userId, String title, String description, Instant targetDate);

    List<Goal> getActiveGoals(UUID userId);

    Goal completeGoal(UUID goalId);

    Milestone addMilestone(UUID goalId, String title, Milestone.Period period);

    Task addTask(UUID goalId, UUID milestoneId, String title, Instant dueDate, Integer estimatedMinutes);

    List<Task> getNextSteps(UUID userId);

    Task completeTask(UUID taskId);
}
