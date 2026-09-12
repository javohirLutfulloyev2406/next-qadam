package uz.nextqadam.bot.nudge;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import uz.nextqadam.bot.common.enums.ToneType;

public interface NudgeService {

    Reminder scheduleReminder(UUID userId, UUID taskId, Instant scheduledAt, ToneType tone);

    void cancelReminder(UUID reminderId);

    List<Reminder> getPendingReminders();

    Reminder snoozeReminder(UUID reminderId, Instant newScheduledAt);
}
