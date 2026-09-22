package uz.nextqadam.bot.admin;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ActivityLogCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(ActivityLogCleanupScheduler.class);

    private static final int RETENTION_DAYS = 60;

    private final UserActivityLogRepository userActivityLogRepository;

    public ActivityLogCleanupScheduler(UserActivityLogRepository userActivityLogRepository) {
        this.userActivityLogRepository = userActivityLogRepository;
    }

    /**
     * Har kuni tunda 03:30'da (kam yuklama vaqtida) 60 kundan eski faoliyat yozuvlarini o'chiradi —
     * jadval cheksiz o'sib ketmasligi uchun. Xato bo'lsa faqat log yoziladi, ilova ishlashiga
     * ta'sir qilmaydi.
     */
    @Scheduled(cron = "0 30 3 * * *", zone = "Asia/Tashkent")
    @Transactional
    public void cleanupOldActivityLogs() {
        try {
            Instant cutoff = Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS);
            int deletedCount = userActivityLogRepository.deleteByCreatedAtBefore(cutoff);
            log.info("Eski faoliyat yozuvlari tozalandi. o'chirilgan={}, cutoff={}", deletedCount, cutoff);
        } catch (Exception e) {
            log.warn("Faoliyat loglarini tozalashda xatolik yuz berdi.", e);
        }
    }
}
