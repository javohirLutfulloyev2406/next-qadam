package uz.nextqadam.bot.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import uz.nextqadam.bot.common.telegram.TelegramBotProperties;
import uz.nextqadam.bot.common.telegram.TelegramExecutor;

@Component
public class DailyAdminDigestScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyAdminDigestScheduler.class);

    private final AdminService adminService;
    private final TelegramExecutor telegramExecutor;
    private final TelegramBotProperties telegramBotProperties;

    public DailyAdminDigestScheduler(AdminService adminService, TelegramExecutor telegramExecutor,
                                      TelegramBotProperties telegramBotProperties) {
        this.adminService = adminService;
        this.telegramExecutor = telegramExecutor;
        this.telegramBotProperties = telegramBotProperties;
    }

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Tashkent")
    public void sendDailyDigest() {
        Long adminGroupId = telegramBotProperties.getAdminGroupId();
        if (adminGroupId == null || adminGroupId == 0L) {
            log.info("Admin group sozlanmagan, kunlik hisobot yuborilmadi.");
            return;
        }

        SystemStats stats = adminService.getSystemStats();
        String text = """
                🌅 <b>Kunlik hisobot</b>

                👥 Jami: %d (+%d bugun)
                🔥 Faol: %d
                🎯 Faol maqsadlar: %d
                ✅ Kecha bajarilgan: %d
                ⚠️ Xatolar (24s): %d"""
                .formatted(stats.totalUsers(), stats.newUsersToday(), stats.activeUsersToday(),
                        stats.totalActiveGoals(), stats.tasksDoneToday(), stats.errorsLast24h());
        telegramExecutor.sendMessage(adminGroupId, text);
    }
}
