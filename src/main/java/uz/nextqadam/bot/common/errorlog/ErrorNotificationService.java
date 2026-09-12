package uz.nextqadam.bot.common.errorlog;

public interface ErrorNotificationService {

    // TODO: ErrorLogEntity yaratib saqlash va admin guruhga TelegramExecutor orqali xabar yuborish
    void logAndNotify(Exception e, String sourceModule, String logId);
}