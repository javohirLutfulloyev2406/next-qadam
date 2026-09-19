package uz.nextqadam.bot.track;

import java.util.UUID;

import uz.nextqadam.bot.ai.dto.EveningCheckinResult;
import uz.nextqadam.bot.ai.dto.GoalDriftResult;
import uz.nextqadam.bot.ai.dto.WeeklyRetrospective;

public interface TrackService {

    /**
     * Kechki check-in matnini AI orqali tahlil qiladi, bajarilgan deb topilgan Task'larni DONE qiladi
     * va CheckIn(EVENING) sifatida saqlaydi. AI tahlili muvaffaqiyatsiz bo'lsa, CheckIn baribir
     * "[AI_PARSE_FAILED]" belgisi bilan saqlanadi va {@code null} qaytariladi — chaqiruvchi
     * (TrackHandler) bu holatda foydalanuvchiga neytral javob ko'rsatishi kerak.
     */
    EveningCheckinResult processEveningCheckin(UUID userId, String rawText);

    WeeklyRetrospective generateWeeklyRetrospective(UUID userId);

    /**
     * Faol Goal umuman bo'lmasa, AI chaqirilmasdan matchScore=100 bilan erta qaytadi.
     */
    GoalDriftResult checkGoalDrift(UUID userId);

    WeeklyStats getWeeklyStats(UUID userId);

    record WeeklyStats(int totalTasks, int doneTasks) {
    }
}
