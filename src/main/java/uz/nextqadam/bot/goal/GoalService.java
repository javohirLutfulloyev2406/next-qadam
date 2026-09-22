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

    /**
     * AI_DECOMPOSITION_FAILURE_MARKER'ning aniqroq varianti — AI provayder vaqtinchalik band
     * bo'lgani (503/502/429 yoki timeout, retry'lar ham tugagan) sababli muvaffaqiyatsiz bo'lgan
     * hollarda qo'shiladi. GoalHandler shu belgi orqali foydalanuvchiga umumiy xabar o'rniga
     * aniqroq signal ("AI hozircha band...") ko'rsatadi.
     */
    String AI_DECOMPOSITION_TRANSIENT_FAILURE_MARKER = "[AI_DECOMPOSITION_FAILED_TRANSIENT]";

    /**
     * Brain Dump'dan kelgan, aniq Goal'ga bog'lanmagan tasklar uchun "quti" vazifasini bajaruvchi
     * maxsus Goal'ning nomi (Task.goal har doim majburiy bo'lgani uchun kerak).
     */
    String DAILY_CATCH_ALL_GOAL_TITLE = "📥 Kundalik ishlar";

    Goal createGoalWithAiDecomposition(UUID userId, String rawDescription);

    /**
     * Foydalanuvchining DAILY_CATCH_ALL_GOAL_TITLE nomli maxsus Goal'ini qaytaradi — mavjud bo'lmasa
     * ACTIVE holatda yaratadi.
     */
    Goal getOrCreateDailyCatchAllGoal(UUID userId);

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
