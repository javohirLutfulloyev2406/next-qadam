package uz.nextqadam.bot.common.errorlog.impl;

import org.springframework.stereotype.Service;

import uz.nextqadam.bot.common.errorlog.ErrorLogRepository;
import uz.nextqadam.bot.common.errorlog.ErrorNotificationService;
import uz.nextqadam.bot.common.telegram.TelegramExecutor;

@Service
public class ErrorNotificationServiceImpl implements ErrorNotificationService {

    private final ErrorLogRepository errorLogRepository;
    private final TelegramExecutor telegramExecutor;

    public ErrorNotificationServiceImpl(ErrorLogRepository errorLogRepository, TelegramExecutor telegramExecutor) {
        this.errorLogRepository = errorLogRepository;
        this.telegramExecutor = telegramExecutor;
    }

    @Override
    public void logAndNotify(Exception e, String sourceModule, String logId) {
        // TODO: implementatsiya
    }
}