package uz.nextqadam.bot.common.telegram;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import uz.nextqadam.bot.admin.AdminHandler;
import uz.nextqadam.bot.common.AdminAuthService;
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
    private final AdminHandler adminHandler;
    private final AdminAuthService adminAuthService;

    public UpdateDispatcher(OnboardingHandler onboardingHandler, GoalHandler goalHandler,
                             ProfileHandler profileHandler, MemoryHandler memoryHandler, PlanHandler planHandler,
                             CompanionHandler companionHandler, TrackHandler trackHandler, NudgeHandler nudgeHandler,
                             ErrorNotificationService errorNotificationService, AdminHandler adminHandler,
                             AdminAuthService adminAuthService) {
        this.onboardingHandler = onboardingHandler;
        this.goalHandler = goalHandler;
        this.profileHandler = profileHandler;
        this.memoryHandler = memoryHandler;
        this.planHandler = planHandler;
        this.companionHandler = companionHandler;
        this.trackHandler = trackHandler;
        this.nudgeHandler = nudgeHandler;
        this.errorNotificationService = errorNotificationService;
        this.adminHandler = adminHandler;
        this.adminAuthService = adminAuthService;
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
                case HELP -> companionHandler.handleHelpCommand(update);
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
                // Dispatcher darajasidagi tekshiruv — handler ichida yana bir marta (ikkinchi qatlam)
                // tekshiriladi. Admin bo'lmasa handler o'zi neytral javob qaytaradi, shu sababli bu yerda
                // shart-sharoitsiz yo'naltiramiz.
                case ADMIN -> adminHandler.handleAdminCommand(update);
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

        // Admin AWAITING_* holatlari — ikki qatlamli himoya: dispatcher bu yerda tekshiradi, handler
        // metodi ichida yana bir bor tekshiriladi. Admin bo'lmagan foydalanuvchi (masalan admin ro'yxatidan
        // olib tashlangandan keyin eski holat qolib ketgan bo'lsa) hech qanday javob olmaydi.
        if (adminHandler.isAwaitingBroadcastText(chatId)) {
            if (adminAuthService.isAdmin(chatId)) {
                adminHandler.handleBroadcastText(update);
            } else {
                log.warn("[{}] Ruxsatsiz admin urinish: telegramId={}", logId, chatId);
            }
            return;
        }

        if (adminHandler.isAwaitingSearchQuery(chatId)) {
            if (adminAuthService.isAdmin(chatId)) {
                adminHandler.handleSearchText(update);
            } else {
                log.warn("[{}] Ruxsatsiz admin urinish: telegramId={}", logId, chatId);
            }
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

        // Eng muhim xavfsizlik nuqtasi: ADMIN_* callbackData'ni faqat admin yubora oladi. Bu yerdagi
        // tekshiruv — handler ichidagi (ikkinchi qatlam) tekshiruvdan mustaqil, birinchi qatlam. Admin
        // bo'lmagan foydalanuvchi bu callbackData'ni qalbakilashtirsa (masalan eski xabarni forward
        // qilib), hech qanday ta'sir qilmaydi va OGOHLANTIRUVCHI darajada log yoziladi.
        if (data != null && data.startsWith("ADMIN_")) {
            if (!adminAuthService.isAdmin(chatId)) {
                log.warn("[{}] Ruxsatsiz admin urinish: telegramId={}", logId, chatId);
                return;
            }
            if (adminHandler.isStatsCallback(data)) {
                adminHandler.handleStatsCallback(update);
            } else if (adminHandler.isBroadcastStartCallback(data)) {
                adminHandler.handleBroadcastStartCallback(update);
            } else if (adminHandler.isBroadcastConfirmCallback(data)) {
                adminHandler.handleBroadcastConfirmCallback(update);
            } else if (adminHandler.isBroadcastCancelCallback(data)) {
                adminHandler.handleBroadcastCancelCallback(update);
            } else if (adminHandler.isSearchStartCallback(data)) {
                adminHandler.handleSearchStartCallback(update);
            } else if (adminHandler.isErrorsCallback(data)) {
                adminHandler.handleErrorsCallback(update);
            } else if (adminHandler.isRefreshCallback(data)) {
                adminHandler.handleRefreshCallback(update);
            }
            return;
        }

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