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

    /**
     * AI javobini kutish paytida ko'rsatiladigan placeholder xabar — TelegramExecutor.
     * sendPlaceholder() orqali yuboriladi, natija tayyor bo'lgach editMessageText bilan haqiqiy
     * javobga almashtiriladi. Telegram'ning o'zi ko'rsatadigan "typing..." statusi atigi 5
     * soniyadan keyin o'chib qolgani uchun, uzoqroq AI javoblarida foydalanuvchi botni
     * "osilib qoldi" deb o'ylamasligi uchun kerak.
     */
    String typingPlaceholder(ToneType tone);
}
