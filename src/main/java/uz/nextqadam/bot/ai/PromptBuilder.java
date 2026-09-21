package uz.nextqadam.bot.ai;

import java.util.List;

import uz.nextqadam.bot.ai.dto.TaskSummaryForPrompt;
import uz.nextqadam.bot.common.enums.Language;
import uz.nextqadam.bot.common.enums.ToneType;

public interface PromptBuilder {

    /**
     * JSON struktura kalitlari (title, milestones, tasks, period, estimatedMinutes) har doim INGLIZCHA
     * qoladi — bu dastur ichki formati va AiResponseParser aynan shu kalitlarni kutadi. Faqat JSON
     * ICHIDAGI QIYMATLAR (milestone/task nomlari) {@code language}ga mos tilda bo'lishi kerak.
     */
    String buildGoalDecompositionPrompt(String goalDescription, String memoryContext, Language language);

    String buildBrainDumpPrompt(String rawText, Language language);

    /**
     * Foydalanuvchining tonega mos "shaxsiyat"i bilan erkin suhbat uchun system prompt yaratadi.
     * memoryContext va recentGoalsSummary bo'sh bo'lmasa, "Foydalanuvchi haqida bilganlaringiz"
     * blokiga qo'shiladi.
     */
    String buildFreeChatSystemPrompt(ToneType tone, String memoryContext, String recentGoalsSummary, Language language);

    /**
     * Foydalanuvchining haqiqiy tarixiga (oxirgi bajarilgan task, faol goal, bajarilgan tasklar soni)
     * asoslangan, tanlangan tonga mos motivatsion xabar so'raydigan prompt yaratadi.
     */
    String buildMotivationPrompt(ToneType tone, String lastCompletedTask, String activeGoalTitle,
                                  int completedTaskCount, Language language);

    /**
     * Joriy Task'ni aniq 5 daqiqada bajarish mumkin bo'lgan bitta kichik, konkret harakatga
     * qisqartirishni so'raydigan prompt yaratadi.
     */
    String buildSosPrompt(ToneType tone, String currentTaskTitle, int estimatedMinutes, Language language);

    /**
     * Foydalanuvchining kechqurun yozgan erkin matnini bugungi task ro'yxati bilan solishtirib, har bir
     * task uchun done/reason klassifikatsiyasini so'raydigan prompt yaratadi.
     */
    String buildEveningCheckinPrompt(String rawText, List<TaskSummaryForPrompt> todaysTasks, Language language);

    /**
     * Haftalik statistika (jami/bajarilgan) va task sarlavhalari asosida qisqa xulosa va bitta tavsiya
     * so'raydigan prompt yaratadi.
     */
    String buildWeeklyRetrospectivePrompt(int totalTasks, int doneTasks, List<String> completedTaskTitles,
                                           List<String> missedTaskTitles, Language language);

    /**
     * Faol Goal sarlavhasini oxirgi 14 kunlik Task sarlavhalari bilan solishtirib, 0-100 moslik balli
     * so'raydigan prompt yaratadi.
     */
    String buildGoalDriftPrompt(String goalTitle, List<String> recentTaskTitles, Language language);
}
