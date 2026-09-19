package uz.nextqadam.bot.nudge;

import java.util.UUID;

public interface NudgeService {

    /**
     * Task'ni SNOOZED qiladi, consecutiveSnoozeCount'ni oshiradi va snoozedUntil = now + 1 soat
     * belgilaydi. Agar shu bilan consecutiveSnoozeCount 3 taga yetsa, estimatedMinutes'ni 3'ga bo'lib
     * (kamida 5 daqiqa) Task'ni darhol PENDING'ga qaytaradi va sanoqni nolga tushiradi
     * (adaptiveShrinkApplied=true).
     */
    SnoozeResult snoozeTask(UUID taskId);

    /**
     * status=SNOOZED va snoozedUntil allaqachon o'tgan barcha Task'larni PENDING'ga qaytaradi.
     */
    void requeueDueSnoozedTasks();
}
