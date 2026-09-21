package uz.nextqadam.bot.companion;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import uz.nextqadam.bot.common.HtmlEscaper;
import uz.nextqadam.bot.common.LocalizationService;
import uz.nextqadam.bot.common.MessageTemplateService;
import uz.nextqadam.bot.common.enums.Language;
import uz.nextqadam.bot.common.keyboard.KeyboardService;
import uz.nextqadam.bot.common.telegram.TelegramExecutor;
import uz.nextqadam.bot.goal.GoalService;
import uz.nextqadam.bot.goal.Task;
import uz.nextqadam.bot.user.User;
import uz.nextqadam.bot.user.UserService;

@Component
public class CompanionHandler {

    private final CompanionService companionService;
    private final UserService userService;
    private final GoalService goalService;
    private final KeyboardService keyboardService;
    private final TelegramExecutor telegramExecutor;
    private final MessageTemplateService messageTemplateService;
    private final LocalizationService localizationService;

    public CompanionHandler(CompanionService companionService, UserService userService, GoalService goalService,
                             KeyboardService keyboardService, TelegramExecutor telegramExecutor,
                             MessageTemplateService messageTemplateService, LocalizationService localizationService) {
        this.companionService = companionService;
        this.userService = userService;
        this.goalService = goalService;
        this.keyboardService = keyboardService;
        this.telegramExecutor = telegramExecutor;
        this.messageTemplateService = messageTemplateService;
        this.localizationService = localizationService;
    }

    public void handleHelpCommand(Update update) {
        Long chatId = update.getMessage().getChatId();
        Language language = userService.findByTelegramId(chatId).map(User::getLanguage).orElse(Language.UZ);
        telegramExecutor.sendMessageWithKeyboard(chatId,
                localizationService.get(language, "help.guide.prompt"),
                keyboardService.buildGuideLinkKeyboard(language));
    }

    public void handleMotivateCommand(Update update) {
        Long chatId = update.getMessage().getChatId();
        User user = userService.findByTelegramId(chatId).orElse(null);
        if (user == null) {
            telegramExecutor.sendMessage(chatId, localizationService.get(Language.UZ, "common.please_start"));
            return;
        }

        Integer placeholderMessageId = telegramExecutor.sendPlaceholder(chatId,
                messageTemplateService.typingPlaceholder(user.getLanguage(), user.getTonePreference()));
        String motivation = companionService.generateMotivation(user.getId());
        showResult(chatId, placeholderMessageId, "🔥 " + HtmlEscaper.escape(motivation), null);
    }

    public void handleSosCommand(Update update) {
        Long chatId = update.getMessage().getChatId();
        User user = userService.findByTelegramId(chatId).orElse(null);
        if (user == null) {
            telegramExecutor.sendMessage(chatId, localizationService.get(Language.UZ, "common.please_start"));
            return;
        }

        Task currentTask = goalService.getCurrentTaskForUser(user.getId()).orElse(null);
        if (currentTask == null) {
            telegramExecutor.sendMessage(chatId, localizationService.get(user.getLanguage(), "sos.no_task"));
            return;
        }

        Integer placeholderMessageId = telegramExecutor.sendPlaceholder(chatId,
                messageTemplateService.typingPlaceholder(user.getLanguage(), user.getTonePreference()));
        String microStep = companionService.generateSosMicroStep(user.getId());

        String text = localizationService.get(user.getLanguage(), "sos.text", HtmlEscaper.escape(microStep));

        showResult(chatId, placeholderMessageId, text,
                keyboardService.buildTaskActionKeyboard(currentTask.getId(), user.getLanguage()));
    }

    public void handleFreeChat(Update update) {
        Message message = update.getMessage();
        Long chatId = message.getChatId();

        User user = userService.findByTelegramId(chatId).orElse(null);
        if (user == null) {
            telegramExecutor.sendMessage(chatId, localizationService.get(Language.UZ, "common.please_start"));
            return;
        }

        Integer placeholderMessageId = telegramExecutor.sendPlaceholder(chatId,
                messageTemplateService.typingPlaceholder(user.getLanguage(), user.getTonePreference()));
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
