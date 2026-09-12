package uz.nextqadam.bot.nudge;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RetrospectiveScheduler {

    // TODO: haftalik/oylik retrospektiva xabarlarini generatsiya qilib yuborish
    @Scheduled(cron = "0 0 20 * * SUN")
    public void runWeeklyRetrospective() {
    }
}
