package uz.nextqadam.bot.nudge;

import uz.nextqadam.bot.goal.Task;

public record SnoozeResult(Task task, boolean adaptiveShrinkApplied, int newEstimatedMinutes) {
}
