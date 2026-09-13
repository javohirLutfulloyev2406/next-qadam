package uz.nextqadam.bot.common.telegram;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import uz.nextqadam.bot.common.BotCommand;
import uz.nextqadam.bot.common.errorlog.ErrorNotificationService;
import uz.nextqadam.bot.goal.GoalHandler;
import uz.nextqadam.bot.user.OnboardingHandler;

@Component
public class UpdateDispatcher {

    private static final Logger log = LoggerFactory.getLogger(UpdateDispatcher.class);

    private final OnboardingHandler onboardingHandler;
    private final GoalHandler goalHandler;
    private final ErrorNotificationService errorNotificationService;

    public UpdateDispatcher(OnboardingHandler onboardingHandler, GoalHandler goalHandler,
                             ErrorNotificationService errorNotificationService) {
        this.onboardingHandler = onboardingHandler;
        this.goalHandler = goalHandler;
        this.errorNotificationService = errorNotificationService;
    }

    public void dispatch(Update update) {
        String logId = UUID.randomUUID().toString();
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                dispatchMessage(update, logId);
            } else if (update.hasCallbackQuery()) {
                dispatchCallbackQuery(update, logId);
            }
        } catch (Exception e) {
            errorNotificationService.logAndNotify(e, "UpdateDispatcher", logId);
        }
    }

    private void dispatchMessage(Update update, String logId) {
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        String text = message.getText();

        Optional<BotCommand> command = BotCommand.fromText(text);
        if (command.isPresent()) {
            switch (command.get()) {
                case START -> onboardingHandler.handleStart(update);
                case NEW_GOAL -> goalHandler.handleNewGoalCommand(update);
                case NEXT_STEP -> goalHandler.handleNextStepCommand(update);
                default -> log.info("[{}] TODO: keyingi modulga ulanadi. command={}, chatId={}", logId, command.get(), chatId);
            }
            return;
        }

        // Avval OnboardingHandler holatini tekshiramiz, keyin GoalHandler holatini —
        // bir vaqtning o'zida faqat bitta oqim faol bo'lishi kutiladi.
        if (onboardingHandler.isAwaitingName(chatId)) {
            onboardingHandler.handleName(update);
            return;
        }

        if (goalHandler.isAwaitingGoalDescription(chatId)) {
            goalHandler.handleGoalDescription(update);
            return;
        }

        log.info("[{}] TODO: keyingi modulga ulanadi. matn qabul qilindi, chatId={}", logId, chatId);
    }

    private void dispatchCallbackQuery(Update update, String logId) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();
        String data = callbackQuery.getData();

        if (onboardingHandler.isAwaitingTone(chatId) && onboardingHandler.isToneCallback(data)) {
            onboardingHandler.handleToneSelection(update);
            return;
        }

        log.info("[{}] TODO: keyingi modulga ulanadi. callbackData={}, chatId={}", logId, data, chatId);
    }
}