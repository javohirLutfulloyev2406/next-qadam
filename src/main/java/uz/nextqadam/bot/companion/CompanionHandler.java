package uz.nextqadam.bot.companion;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import uz.nextqadam.bot.common.HtmlEscaper;
import uz.nextqadam.bot.common.MessageTemplateService;
import uz.nextqadam.bot.common.keyboard.KeyboardService;
import uz.nextqadam.bot.common.telegram.TelegramExecutor;
import uz.nextqadam.bot.goal.GoalService;
import uz.nextqadam.bot.goal.Task;
import uz.nextqadam.bot.user.User;
import uz.nextqadam.bot.user.UserService;

@Component
public class CompanionHandler {

    private static final String NOT_REGISTERED_MESSAGE = "Avval /start orqali ro'yxatdan o'ting.";

    private final CompanionService companionService;
    private final UserService userService;
    private final GoalService goalService;
    private final KeyboardService keyboardService;
    private final TelegramExecutor telegramExecutor;
    private final MessageTemplateService messageTemplateService;

    public CompanionHandler(CompanionService companionService, UserService userService, GoalService goalService,
                             KeyboardService keyboardService, TelegramExecutor telegramExecutor,
                             MessageTemplateService messageTemplateService) {
        this.companionService = companionService;
        this.userService = userService;
        this.goalService = goalService;
        this.keyboardService = keyboardService;
        this.telegramExecutor = telegramExecutor;
        this.messageTemplateService = messageTemplateService;
    }

    public void handleHelpCommand(Update update) {
        Long chatId = update.getMessage().getChatId();
        telegramExecutor.sendMessageWithKeyboard(chatId,
                "📖 Botdan qanday foydalanish haqida to'liq qo'llanma tayyorladik.\n\n"
                        + "Pastdagi tugma orqali oching — barcha buyruqlar, misollar va tushuntirishlar bilan.",
                keyboardService.buildGuideLinkKeyboard());
    }

    public void handleMotivateCommand(Update update) {
        Long chatId = update.getMessage().getChatId();
        User user = userService.findByTelegramId(chatId).orElse(null);
        if (user == null) {
            telegramExecutor.sendMessage(chatId, NOT_REGISTERED_MESSAGE);
            return;
        }

        Integer placeholderMessageId = telegramExecutor.sendPlaceholder(chatId,
                messageTemplateService.typingPlaceholder(user.getTonePreference()));
        String motivation = companionService.generateMotivation(user.getId());
        showResult(chatId, placeholderMessageId, "🔥 " + HtmlEscaper.escape(motivation), null);
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

        Integer placeholderMessageId = telegramExecutor.sendPlaceholder(chatId,
                messageTemplateService.typingPlaceholder(user.getTonePreference()));
        String microStep = companionService.generateSosMicroStep(user.getId());

        String text = "🆘 <b>Yaxshi, sekinroq boramiz.</b>\n\n"
                + "Faqat shuni qil:\n"
                + "👉 " + HtmlEscaper.escape(microStep) + "\n\n"
                + "Shuning o'zi kifoya. Qolganini keyin o'ylaymiz.";

        showResult(chatId, placeholderMessageId, text, keyboardService.buildTaskActionKeyboard(currentTask.getId()));
    }

    public void handleFreeChat(Update update) {
        Message message = update.getMessage();
        Long chatId = message.getChatId();

        User user = userService.findByTelegramId(chatId).orElse(null);
        if (user == null) {
            telegramExecutor.sendMessage(chatId, NOT_REGISTERED_MESSAGE);
            return;
        }

        Integer placeholderMessageId = telegramExecutor.sendPlaceholder(chatId,
                messageTemplateService.typingPlaceholder(user.getTonePreference()));
        String reply = companionService.generateFreeChatReply(user.getId(), message.getText());
        showResult(chatId, placeholderMessageId, HtmlEscaper.escape(reply), null);
    }

    /**
     * Placeholder xabarni yakuniy matn bilan (kerak bo'lsa tugma bilan) almashtiradi. Agar
     * placeholder biror sababga ko'ra yuborilmagan bo'lsa (messageId == null), oddiy yangi xabar
     * yuboriladi — placeholder hech qachon o'zgarishsiz osilib qolmasligi kerak.
     */
    private void showResult(Long chatId, Integer placeholderMessageId, String text, InlineKeyboardMarkup keyboard) {
        if (placeholderMessageId != null) {
            telegramExecutor.editMessageText(chatId, placeholderMessageId, text);
            if (keyboard != null) {
                telegramExecutor.editMessageReplyMarkup(chatId, placeholderMessageId, keyboard);
            }
        } else if (keyboard != null) {
            telegramExecutor.sendMessageWithKeyboard(chatId, text, keyboard);
        } else {
            telegramExecutor.sendMessage(chatId, text);
        }
    }
}
