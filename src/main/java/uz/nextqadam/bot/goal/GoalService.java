package uz.nextqadam.bot.goal;

import java.util.List;
import java.util.Map;
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

    /**
     * getNextStep'ga o'xshaydi, lekin foydalanuvchining BARCHA faol Goal'lari orasidan emas,
     * aynan bitta Goal doirasidagi eng yaqin PENDING Task'ini qaytaradi (masalan /goals
     * panelidagi "Keyingi qadamni ko'rish" tugmasi uchun).
     */
    Optional<Task> getNextStepForGoal(UUID goalId);

    /**
     * getNextStep bilan bir xil logikani ishlatadi — foydalanuvchining eng yaqin
     * PENDING Task'ini qaytaradi. "Joriy vazifa"ni aniqroq ifodalash uchun alohida
     * nom bilan taqdim etiladi (masalan /done oqimida ishlatiladi).
     */
    Optional<Task> getCurrentTaskForUser(UUID userId);

    Task markTaskDone(UUID taskId);

    List<Goal> getActiveGoals(UUID userId);

    /**
     * Har bir faol Goal uchun uning Task'lari bo'yicha hisoblangan progress statistikasi
     * (doneCount/totalCount/percentComplete). getActiveGoals bilan bir xil tartibda qaytadi.
     */
    Map<Goal, ProgressStats> getGoalsWithProgress(UUID userId);

    record ProgressStats(int doneCount, int totalCount, int percentComplete) {
    }
}
