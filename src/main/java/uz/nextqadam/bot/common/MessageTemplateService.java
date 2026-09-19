package uz.nextqadam.bot.common;

import uz.nextqadam.bot.common.enums.ToneType;

public interface MessageTemplateService {

    String welcomeAfterTone(ToneType tone, String userName);

    String taskDoneCongrats(ToneType tone, String taskTitle);

    String noPendingTask(ToneType tone);

    String goalDecompositionIntro(ToneType tone);

    /**
     * Muddati kelgan Reminder yoki kunlik ustuvorlik nudge'i uchun matn — taskTitle o'rniga
     * Reminder.content ham berilishi mumkin (masalan task'ga bog'lanmagan eslatmalar uchun).
     */
    String reminderNudge(ToneType tone, String taskTitle);

    /**
     * "⏰ Keyinroq" tugmasi bosilganda, oddiy (adaptive-shrink qo'llanilmagan) holatda ko'rsatiladigan
     * tasdiqlash matni.
     */
    String snoozeAck(ToneType tone);

    /**
     * Task 3 marta ketma-ket kechiktirilib, avtomatik kichraytirilganda ko'rsatiladigan xabar.
     */
    String adaptiveShrinkNotice(ToneType tone, int newEstimatedMinutes);
}
