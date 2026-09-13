package uz.nextqadam.bot.goal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalService {

    /**
     * Goal.description ichiga qo'shiladigan belgi — AI orqali bosqichlarga bo'lish
     * muvaffaqiyatsiz bo'lgan hollarda Goal baribir ACTIVE holda saqlanadi, lekin
     * milestone/task'siz qoladi. Chaqiruvchi (masalan GoalHandler) shu belgi orqali
     * bu holatni aniqlab, foydalanuvchiga tegishli xabar ko'rsatishi mumkin.
     */
    String AI_DECOMPOSITION_FAILURE_MARKER = "[AI_DECOMPOSITION_FAILED]";

    Goal createGoalWithAiDecomposition(UUID userId, String rawDescription);

    Optional<Task> getNextStep(UUID userId);

    Task markTaskDone(UUID taskId);

    List<Goal> getActiveGoals(UUID userId);
}
