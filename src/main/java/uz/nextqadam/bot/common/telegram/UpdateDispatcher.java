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
import uz.nextqadam.bot.companion.CompanionHandler;
import uz.nextqadam.bot.goal.GoalHandler;
import uz.nextqadam.bot.memory.MemoryHandler;
import uz.nextqadam.bot.nudge.NudgeHandler;
import uz.nextqadam.bot.plan.PlanHandler;
import uz.nextqadam.bot.track.TrackHandler;
import uz.nextqadam.bot.user.OnboardingHandler;
import uz.nextqadam.bot.user.ProfileHandler;

@Component
public class UpdateDispatcher {

    private static final Logger log = LoggerFactory.getLogger(UpdateDispatcher.class);

    private final OnboardingHandler onboardingHandler;
    private final GoalHandler goalHandler;
    private final ProfileHandler profileHandler;
    private final MemoryHandler memoryHandler;
    private final PlanHandler planHandler;
    private final CompanionHandler companionHandler;
    private final TrackHandler trackHandler;
    private final NudgeHandler nudgeHandler;
    private final ErrorNotificationService errorNotificationService;

    public UpdateDispatcher(OnboardingHandler onboardingHandler, GoalHandler goalHandler,
                             ProfileHandler profileHandler, MemoryHandler memoryHandler, PlanHandler planHandler,
                             CompanionHandler companionHandler, TrackHandler trackHandler, NudgeHandler nudgeHandler,
                             ErrorNotificationService errorNotificationService) {
        this.onboardingHandler = onboardingHandler;
        this.goalHandler = goalHandler;
        this.profileHandler = profileHandler;
        this.memoryHandler = memoryHandler;
        this.planHandler = planHandler;
        this.companionHandler = companionHandler;
        this.trackHandler = trackHandler;
        this.nudgeHandler = nudgeHandler;
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

        Optional<BotCommand> command = (text != null && text.startsWith("/"))
                ? BotCommand.fromText(text)
                : BotCommand.fromButtonLabel(text);
        if (command.isPresent()) {
            switch (command.get()) {
                case START -> onboardingHandler.handleStart(update);
                case NEW_GOAL -> goalHandler.handleNewGoalCommand(update);
                case NEXT_STEP -> goalHandler.handleNextStepCommand(update);
                case DONE -> goalHandler.handleDoneCommand(update);
                case GOALS -> goalHandler.handleGoalsCommand(update);
                case PROFILE -> profileHandler.handleProfileCommand(update);
                case MEMORY -> memoryHandler.handleMemoryCommand(update);
                case FORGET -> memoryHandler.handleForgetCommand(update);
                case PLAN_DAY -> planHandler.handlePlanDayCommand(update);
                case BRAIN_DUMP -> planHandler.handleBrainDumpCommand(update);
                case IDEAS -> planHandler.handleIdeasCommand(update);
                case MOTIVATE -> companionHandler.handleMotivateCommand(update);
                case SOS -> companionHandler.handleSosCommand(update);
                case EVENING_CHECKIN -> trackHandler.handleEveningCheckinCommand(update);
                case TEST_RETRO -> trackHandler.handleTestRetroCommand(update);
                case TEST_DRIFT -> trackHandler.handleTestDriftCommand(update);
                case TEST_NUDGE -> nudgeHandler.handleTestNudgeCommand(update);
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

        if (profileHandler.isAwaitingProfileName(chatId)) {
            profileHandler.handleNameInput(update);
            return;
        }

        if (profileHandler.isAwaitingProfileTimezone(chatId)) {
            profileHandler.handleTimezoneInput(update);
            return;
        }

        if (planHandler.isAwaitingBrainDumpText(chatId)) {
            planHandler.handleBrainDumpText(update);
            return;
        }

        if (trackHandler.isAwaitingEveningCheckinText(chatId)) {
            trackHandler.handleEveningCheckinText(update);
            return;
        }

        // Yakuniy fallback: yuqoridagi hech qanday komanda yoki AWAITING_* holatiga mos kelmagan matn —
        // demak bu erkin suhbat. CompanionHandler AI orqali kontekstli javob beradi.
        companionHandler.handleFreeChat(update);
    }

    private void dispatchCallbackQuery(Update update, String logId) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();
        String data = callbackQuery.getData();

        if (onboardingHandler.isToneCallback(data)) {
            if (!onboardingHandler.isAwaitingTone(chatId)) {
                // stageByChatId in-memory xotira — instance qayta ishga tushganda yo'qoladi (pastdagi TODO'ga qarang).
                // Callback data'ning o'zi ("ONBOARDING_TONE_...") tanlovni bir ma'noli aniqlaydi, shu sababli
                // holat yo'qolgan bo'lsa ham handleToneSelection'ga yo'naltiramiz, aks holda foydalanuvchi
                // TODO fallback'ga tushib, onboarding jarayoni to'xtab qoladi.
                log.warn("[{}] Ton callback keldi (callbackData={}), lekin chatId={} uchun AWAITING_TONE holati "
                        + "topilmadi — ehtimol instance qayta ishga tushgan. Baribir handleToneSelection'ga yo'naltirilmoqda.",
                        logId, data, chatId);
            }
            onboardingHandler.handleToneSelection(update);
            return;
        }

        if (profileHandler.isEditNameCallback(data)) {
            profileHandler.handleEditNameCallback(update);
            return;
        }

        if (profileHandler.isEditToneCallback(data)) {
            profileHandler.handleEditToneCallback(update);
            return;
        }

        if (profileHandler.isEditTimezoneCallback(data)) {
            profileHandler.handleEditTimezoneCallback(update);
            return;
        }

        if (profileHandler.isProfileToneCallback(data)) {
            profileHandler.handleToneCallback(update);
            return;
        }

        if (memoryHandler.isMemoryView(data)) {
            memoryHandler.handleMemoryViewCallback(update);
            return;
        }

        if (memoryHandler.isMemoryDeleteAllAsk(data)) {
            memoryHandler.handleDeleteAllAskCallback(update);
            return;
        }

        if (memoryHandler.isMemoryDeleteAllConfirm(data)) {
            memoryHandler.handleDeleteAllConfirmCallback(update);
            return;
        }

        if (memoryHandler.isMemoryDeleteAllCancel(data)) {
            memoryHandler.handleDeleteAllCancelCallback(update);
            return;
        }

        if (memoryHandler.isMemoryDeleteOne(data)) {
            memoryHandler.handleDeleteOneCallback(update);
            return;
        }

        if (goalHandler.isTaskDoneCallback(data)) {
            goalHandler.handleTaskDoneCallback(update);
            return;
        }

        if (nudgeHandler.isTaskSnoozeCallback(data)) {
            nudgeHandler.handleSnoozeCallback(update);
            return;
        }

        if (goalHandler.isGoalsNextStepCallback(data)) {
            goalHandler.handleGoalsNextStepCallback(update);
            return;
        }

        if (planHandler.isCheckinToggleCallback(data)) {
            planHandler.handleCheckinToggle(update);
            return;
        }

        if (planHandler.isCheckinConfirmCallback(data)) {
            planHandler.handleCheckinConfirm(update);
            return;
        }

        if (trackHandler.isDriftUpdateGoalCallback(data)) {
            trackHandler.handleDriftUpdateGoalCallback(update);
            return;
        }

        if (trackHandler.isDriftDismissCallback(data)) {
            trackHandler.handleDriftDismissCallback(update);
            return;
        }

        log.info("[{}] TODO: keyingi modulga ulanadi. callbackData={}, chatId={}", logId, data, chatId);
    }
}