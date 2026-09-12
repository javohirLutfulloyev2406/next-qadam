package uz.nextqadam.bot.nudge;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReminderScheduler {

    // TODO: muddati kelgan PENDING eslatmalarni topib yuborish
    @Scheduled(fixedDelay = 60_000)
    public void dispatchDueReminders() {
    }
}
