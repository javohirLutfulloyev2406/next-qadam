package uz.nextqadam.bot.companion;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import uz.nextqadam.bot.common.HtmlEscaper;
import uz.nextqadam.bot.common.keyboard.KeyboardService;
import uz.nextqadam.bot.common.telegram.TelegramExecutor;
import uz.nextqadam.bot.goal.GoalService;
import uz.nextqadam.bot.goal.Task;
import uz.nextqadam.bot.user.User;
import uz.nextqadam.bot.user.UserService;

@Component
public class CompanionHandler {

    private static final String TYPING_ACTION = "typing";
    private static final String NOT_REGISTERED_MESSAGE = "Avval /start orqali ro'yxatdan o'ting.";

    private final CompanionService companionService;
    private final UserService userService;
    private final GoalService goalService;
    private final KeyboardService keyboardService;
    private final TelegramExecutor telegramExecutor;

    public CompanionHandler(CompanionService companionService, UserService userService, GoalService goalService,
                             KeyboardService keyboardService, TelegramExecutor telegramExecutor) {
        this.companionService = companionService;
        this.userService = userService;
        this.goalService = goalService;
        this.keyboardService = keyboardService;
        this.telegramExecutor = telegramExecutor;
    }

    public void handleMotivateCommand(Update update) {
        Long chatId = update.getMessage().getChatId();
        User user = userService.findByTelegramId(chatId).orElse(null);
        if (user == null) {
            telegramExecutor.sendMessage(chatId, NOT_REGISTERED_MESSAGE);
            return;
        }

        telegramExecutor.sendChatAction(chatId, TYPING_ACTION);
        String motivation = companionService.generateMotivation(user.getId());
        telegramExecutor.sendMessage(chatId, "🔥 " + HtmlEscaper.escape(motivation));
    }

    public void handleSosCommand(Update update) {
        Long chatId = update.getMessage().getChatId();
        User user = userService.findByTelegramId(chatId).orElse(null);
        if (user == null) {
            telegramExecutor.sendMessage(chatId, NOT_REGISTERED_MESSAGE);
            return;
        }

        Task currentTask = goalService.getCurrentTaskForUser(user.getId()).orElse(null);
        if (currentTask == null) {
            telegramExecutor.sendMessage(chatId,
                    "Hozircha faol vazifangiz yo'q, demak SOS kerak emas! /newgoal orqali maqsad qo'shing 🙂");
            return;
        }

        telegramExecutor.sendChatAction(chatId, TYPING_ACTION);
        String microStep = companionService.generateSosMicroStep(user.getId());

        String text = "🆘 <b>Yaxshi, sekinroq boramiz.</b>\n\n"
                + "Faqat shuni qil:\n"
                + "👉 " + HtmlEscaper.escape(microStep) + "\n\n"
                + "Shuning o'zi kifoya. Qolganini keyin o'ylaymiz.";

        telegramExecutor.sendMessageWithKeyboard(chatId, text, keyboardService.buildTaskActionKeyboard(currentTask.getId()));
    }

    public void handleFreeChat(Update update) {
        Message message = update.getMessage();
        Long chatId = message.getChatId();

        User user = userService.findByTelegramId(chatId).orElse(null);
        if (user == null) {
            telegramExecutor.sendMessage(chatId, NOT_REGISTERED_MESSAGE);
            return;
        }

        telegramExecutor.sendChatAction(chatId, TYPING_ACTION);
        String reply = companionService.generateFreeChatReply(user.getId(), message.getText());
        telegramExecutor.sendMessage(chatId, HtmlEscaper.escape(reply));
    }
}
