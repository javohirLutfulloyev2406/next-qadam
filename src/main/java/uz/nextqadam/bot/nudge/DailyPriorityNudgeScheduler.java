package uz.nextqadam.bot.nudge;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import uz.nextqadam.bot.user.User;
import uz.nextqadam.bot.user.UserRepository;

@Component
public class DailyPriorityNudgeScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyPriorityNudgeScheduler.class);

    private final UserRepository userRepository;
    private final NudgeHandler nudgeHandler;
    private final NudgeService nudgeService;

    public DailyPriorityNudgeScheduler(UserRepository userRepository, NudgeHandler nudgeHandler,
                                        NudgeService nudgeService) {
        this.userRepository = userRepository;
        this.nudgeHandler = nudgeHandler;
        this.nudgeService = nudgeService;
    }

    /**
     * Har bir foydalanuvchi uchun agar ustuvor (is_today_priority=true) va hali PENDING Task bo'lsa,
     * eslatma yuboradi. Bunday Task yo'q bo'lsa — hech narsa yuborilmaydi (spam qilinmasin).
     */
    @Scheduled(cron = "0 0 16 * * *", zone = "Asia/Tashkent")
    public void sendDailyPriorityNudges() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            try {
                nudgeHandler.sendPriorityNudgeIfAny(user);
            } catch (Exception e) {
                log.error("Kunlik ustuvorlik nudge'ini yuborishda xatolik. userId={}", user.getId(), e);
            }
        }
    }

    // Butun tizim uchun bitta chaqiruv (user bo'yicha emas) — snooze muddati o'tgan barcha Task'larni
    // qayta PENDING'ga o'tkazadi.
    @Scheduled(fixedDelay = 1_800_000)
    public void requeueDueSnoozedTasks() {
        nudgeService.requeueDueSnoozedTasks();
    }
}
