package uz.nextqadam.bot.goal.impl;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import uz.nextqadam.bot.goal.Goal;
import uz.nextqadam.bot.goal.GoalRepository;
import uz.nextqadam.bot.goal.GoalService;
import uz.nextqadam.bot.goal.Milestone;
import uz.nextqadam.bot.goal.MilestoneRepository;
import uz.nextqadam.bot.goal.Task;
import uz.nextqadam.bot.goal.TaskRepository;

@Service
public class GoalServiceImpl implements GoalService {

    private final GoalRepository goalRepository;
    private final MilestoneRepository milestoneRepository;
    private final TaskRepository taskRepository;

    public GoalServiceImpl(GoalRepository goalRepository, MilestoneRepository milestoneRepository,
                            TaskRepository taskRepository) {
        this.goalRepository = goalRepository;
        this.milestoneRepository = milestoneRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    public Goal createGoal(UUID userId, String title, String description, Instant targetDate) {
        // TODO: implementatsiya
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Goal> getActiveGoals(UUID userId) {
        // TODO: implementatsiya
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Goal completeGoal(UUID goalId) {
        // TODO: implementatsiya
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Milestone addMilestone(UUID goalId, String title, Milestone.Period period) {
        // TODO: implementatsiya
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Task addTask(UUID goalId, UUID milestoneId, String title, Instant dueDate, Integer estimatedMinutes) {
        // TODO: implementatsiya
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Task> getNextSteps(UUID userId) {
        // TODO: implementatsiya
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Task completeTask(UUID taskId) {
        // TODO: implementatsiya
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
