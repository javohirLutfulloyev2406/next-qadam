package uz.nextqadam.bot.nudge.impl;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import uz.nextqadam.bot.common.enums.ToneType;
import uz.nextqadam.bot.nudge.NudgeService;
import uz.nextqadam.bot.nudge.Reminder;
import uz.nextqadam.bot.nudge.ReminderRepository;

@Service
public class NudgeServiceImpl implements NudgeService {

    private final ReminderRepository reminderRepository;

    public NudgeServiceImpl(ReminderRepository reminderRepository) {
        this.reminderRepository = reminderRepository;
    }

    @Override
    public Reminder scheduleReminder(UUID userId, UUID taskId, Instant scheduledAt, ToneType tone) {
        // TODO: implementatsiya
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void cancelReminder(UUID reminderId) {
        // TODO: implementatsiya
    }

    @Override
    public List<Reminder> getPendingReminders() {
        // TODO: implementatsiya
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Reminder snoozeReminder(UUID reminderId, Instant newScheduledAt) {
        // TODO: implementatsiya
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
