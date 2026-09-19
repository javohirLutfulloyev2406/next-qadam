package uz.nextqadam.bot.ai;

import java.util.List;

import uz.nextqadam.bot.ai.dto.TaskSummaryForPrompt;
import uz.nextqadam.bot.common.enums.ToneType;

public interface PromptBuilder {

    String buildGoalDecompositionPrompt(String goalDescription, String memoryContext);

    String buildBrainDumpPrompt(String rawText);

    /**
     * Foydalanuvchining tonega mos "shaxsiyat"i bilan erkin suhbat uchun system prompt yaratadi.
     * memoryContext va recentGoalsSummary bo'sh bo'lmasa, "Foydalanuvchi haqida bilganlaringiz"
     * blokiga qo'shiladi.
     */
    String buildFreeChatSystemPrompt(ToneType tone, String memoryContext, String recentGoalsSummary);

    /**
     * Foydalanuvchining haqiqiy tarixiga (oxirgi bajarilgan task, faol goal, bajarilgan tasklar soni)
     * asoslangan, tanlangan tonga mos motivatsion xabar so'raydigan prompt yaratadi.
     */
    String buildMotivationPrompt(ToneType tone, String lastCompletedTask, String activeGoalTitle,
                                  int completedTaskCount);

    /**
     * Joriy Task'ni aniq 5 daqiqada bajarish mumkin bo'lgan bitta kichik, konkret harakatga
     * qisqartirishni so'raydigan prompt yaratadi.
     */
    String buildSosPrompt(ToneType tone, String currentTaskTitle, int estimatedMinutes);

    /**
     * Foydalanuvchining kechqurun yozgan erkin matnini bugungi task ro'yxati bilan solishtirib, har bir
     * task uchun done/reason klassifikatsiyasini so'raydigan prompt yaratadi.
     */
    String buildEveningCheckinPrompt(String rawText, List<TaskSummaryForPrompt> todaysTasks);

    /**
     * Haftalik statistika (jami/bajarilgan) va task sarlavhalari asosida qisqa xulosa va bitta tavsiya
     * so'raydigan prompt yaratadi.
     */
    String buildWeeklyRetrospectivePrompt(int totalTasks, int doneTasks, List<String> completedTaskTitles,
                                           List<String> missedTaskTitles);

    /**
     * Faol Goal sarlavhasini oxirgi 14 kunlik Task sarlavhalari bilan solishtirib, 0-100 moslik balli
     * so'raydigan prompt yaratadi.
     */
    String buildGoalDriftPrompt(String goalTitle, List<String> recentTaskTitles);
}
