package uz.nextqadam.bot.common.errorlog.impl;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.springframework.stereotype.Service;

import uz.nextqadam.bot.common.errorlog.ErrorLogEntity;
import uz.nextqadam.bot.common.errorlog.ErrorLogRepository;
import uz.nextqadam.bot.common.errorlog.ErrorNotificationService;
import uz.nextqadam.bot.common.telegram.TelegramBotProperties;
import uz.nextqadam.bot.common.telegram.TelegramExecutor;

@Service
public class ErrorNotificationServiceImpl implements ErrorNotificationService {

    private static final int MAX_MESSAGE_LENGTH = 500;

    private final ErrorLogRepository errorLogRepository;
    private final TelegramExecutor telegramExecutor;
    private final TelegramBotProperties telegramBotProperties;

    public ErrorNotificationServiceImpl(ErrorLogRepository errorLogRepository,
                                         TelegramExecutor telegramExecutor,
                                         TelegramBotProperties telegramBotProperties) {
        this.errorLogRepository = errorLogRepository;
        this.telegramExecutor = telegramExecutor;
        this.telegramBotProperties = telegramBotProperties;
    }

    @Override
    public void logAndNotify(Exception e, String sourceModule, String logId) {
        String message = e.getMessage() != null ? e.getMessage() : "";
        String truncatedMessage = message.length() > MAX_MESSAGE_LENGTH
                ? message.substring(0, MAX_MESSAGE_LENGTH) + "..."
                : message;

        ErrorLogEntity errorLog = ErrorLogEntity.builder()
                .sourceModule(sourceModule)
                .exceptionType(e.getClass().getName())
                .message(truncatedMessage)
                .stackTrace(stackTraceOf(e))
                .logId(logId)
                .notified(false)
                .build();
        errorLogRepository.save(errorLog);

        Long adminGroupId = telegramBotProperties.getAdminGroupId();
        if (adminGroupId != null && adminGroupId != 0L) {
            String notificationText = """
                    ⚠️ Xatolik yuz berdi
                    Modul: %s
                    Turi: %s
                    Xabar: %s
                    Log ID: %s
                    """.formatted(sourceModule, e.getClass().getSimpleName(), truncatedMessage, logId);
            telegramExecutor.sendMessage(adminGroupId, notificationText);
        }

        errorLog.setNotified(true);
        errorLogRepository.save(errorLog);
    }

    private String stackTraceOf(Exception e) {
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }
}