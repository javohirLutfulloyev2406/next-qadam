package uz.nextqadam.bot.nudge;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import uz.nextqadam.bot.common.MessageTemplateService;
import uz.nextqadam.bot.common.keyboard.KeyboardService;
import uz.nextqadam.bot.common.telegram.TelegramExecutor;
import uz.nextqadam.bot.goal.Task;

@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final ReminderRepository reminderRepository;
    private final MessageTemplateService messageTemplateService;
    private final KeyboardService keyboardService;
    private final TelegramExecutor telegramExecutor;

    public ReminderScheduler(ReminderRepository reminderRepository, MessageTemplateService messageTemplateService,
                              KeyboardService keyboardService, TelegramExecutor telegramExecutor) {
        this.reminderRepository = reminderRepository;
        this.messageTemplateService = messageTemplateService;
        this.keyboardService = keyboardService;
        this.telegramExecutor = telegramExecutor;
    }

    // TODO: production'da bu interval'ni application.yml orqali sozlanadigan qilish kelajakda
    // yaxshilanish sifatida ko'rib chiqilishi mumkin.
    @Scheduled(fixedDelay = 900_000)
    public void dispatchDueReminders() {
        List<Reminder> dueReminders = reminderRepository.findDueRemindersWithUser(Reminder.Status.PENDING, Instant.now());
        for (Reminder reminder : dueReminders) {
            try {
                sendReminder(reminder);
            } catch (Exception e) {
                log.error("Eslatma yuborishda xatolik. reminderId={}", reminder.getId(), e);
            }
        }
    }

    private void sendReminder(Reminder reminder) {
        String text = messageTemplateService.reminderNudge(reminder.getUser().getLanguage(), reminder.getTone(),
                reminder.getContent());
        Task task = reminder.getTask();

        if (task != null) {
            telegramExecutor.sendMessageWithKeyboard(reminder.getUser().getTelegramId(), text,
                    keyboardService.buildTaskActionKeyboard(task.getId(), reminder.getUser().getLanguage()));
        } else {
            telegramExecutor.sendMessage(reminder.getUser().getTelegramId(), text);
        }

        reminder.setStatus(Reminder.Status.SENT);
        reminderRepository.save(reminder);
    }
}
