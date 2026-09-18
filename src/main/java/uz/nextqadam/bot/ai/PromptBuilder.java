package uz.nextqadam.bot.ai;

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
}
