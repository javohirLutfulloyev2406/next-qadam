package uz.nextqadam.bot.admin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import uz.nextqadam.bot.admin.AdminService.BroadcastResult;
import uz.nextqadam.bot.common.AdminAuthService;
import uz.nextqadam.bot.common.HtmlEscaper;
import uz.nextqadam.bot.common.LocalizationService;
import uz.nextqadam.bot.common.enums.Language;
import uz.nextqadam.bot.common.errorlog.ErrorLogEntity;
import uz.nextqadam.bot.common.keyboard.KeyboardService;
import uz.nextqadam.bot.common.telegram.TelegramExecutor;
import uz.nextqadam.bot.common.util.TimeUtil;
import uz.nextqadam.bot.user.User;
import uz.nextqadam.bot.user.UserRepository.UserSummaryProjection;
import uz.nextqadam.bot.user.UserService;

@Component
public class AdminHandler {

    private static final String STATS_CALLBACK = "ADMIN_STATS";
    private static final String BROADCAST_START_CALLBACK = "ADMIN_BROADCAST_START";
    private static final String SEARCH_START_CALLBACK = "ADMIN_SEARCH_START";
    private static final String ERRORS_CALLBACK = "ADMIN_ERRORS";
    private static final String REFRESH_CALLBACK = "ADMIN_REFRESH";
    private static final String BROADCAST_CONFIRM_CALLBACK = "ADMIN_BROADCAST_CONFIRM";
    private static final String BROADCAST_CANCEL_CALLBACK = "ADMIN_BROADCAST_CANCEL";

    private static final int RECENT_ERRORS_LIMIT = 10;
    private static final int ERROR_MESSAGE_PREVIEW_LENGTH = 100;

    // Xuddi boshqa handler'lardagi kabi — in-memory xotira, instance qayta ishga tushirilganda yo'qoladi.
    private final Map<Long, AdminStage> stageByChatId = new ConcurrentHashMap<>();
    private final Map<Long, String> pendingBroadcastByChatId = new ConcurrentHashMap<>();

    private final AdminAuthService adminAuthService;
    private final AdminService adminService;
    private final UserService userService;
    private final KeyboardService keyboardService;
    private final TelegramExecutor telegramExecutor;
    private final LocalizationService localizationService;

    public AdminHandler(AdminAuthService adminAuthService, AdminService adminService, UserService userService,
                         KeyboardService keyboardService, TelegramExecutor telegramExecutor,
                         LocalizationService localizationService) {
        this.adminAuthService = adminAuthService;
        this.adminService = adminService;
        this.userService = userService;
        this.keyboardService = keyboardService;
        this.telegramExecutor = telegramExecutor;
        this.localizationService = localizationService;
    }

    public boolean isStatsCallback(String callbackData) {
        return STATS_CALLBACK.equals(callbackData);
    }

    public boolean isBroadcastStartCallback(String callbackData) {
        return BROADCAST_START_CALLBACK.equals(callbackData);
    }

    public boolean isSearchStartCallback(String callbackData) {
        return SEARCH_START_CALLBACK.equals(callbackData);
    }

    public boolean isErrorsCallback(String callbackData) {
        return ERRORS_CALLBACK.equals(callbackData);
    }

    public boolean isRefreshCallback(String callbackData) {
        return REFRESH_CALLBACK.equals(callbackData);
    }

    public boolean isBroadcastConfirmCallback(String callbackData) {
        return BROADCAST_CONFIRM_CALLBACK.equals(callbackData);
    }

    public boolean isBroadcastCancelCallback(String callbackData) {
        return BROADCAST_CANCEL_CALLBACK.equals(callbackData);
    }

    public boolean isAwaitingBroadcastText(Long chatId) {
        return stageByChatId.get(chatId) == AdminStage.AWAITING_BROADCAST_TEXT;
    }

    public boolean isAwaitingSearchQuery(Long chatId) {
        return stageByChatId.get(chatId) == AdminStage.AWAITING_SEARCH_QUERY;
    }

    public void handleAdminCommand(Update update) {
        Long chatId = update.getMessage().getChatId();
        Language language = languageOf(chatId);
        // Admin bo'lmagan foydalanuvchiga botning o'zi buyruqni "tanimayapti"dek ko'rsatiladi —
        // bunday buyruq borligini oshkor qilmaslik uchun log ham yozilmaydi.
        if (!adminAuthService.isAdmin(chatId)) {
            telegramExecutor.sendMessage(chatId, localizationService.get(language, "admin.no_such_command"));
            return;
        }
        telegramExecutor.sendMessageWithKeyboard(chatId, localizationService.get(language, "admin.panel.title"),
                keyboardService.buildAdminMenuKeyboard(language));
    }

    public void handleStatsCallback(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();
        if (!adminAuthService.isAdmin(chatId)) {
            return;
        }

        Language language = languageOf(chatId);
        Integer messageId = callbackQuery.getMessage().getMessageId();
        SystemStats stats = adminService.getSystemStats();
        telegramExecutor.editMessageText(chatId, messageId, formatStats(stats, language));
        telegramExecutor.editMessageReplyMarkup(chatId, messageId, keyboardService.buildAdminMenuKeyboard(language));
    }

    public void handleRefreshCallback(Update update) {
        handleStatsCallback(update);
    }

    public void handleBroadcastStartCallback(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        if (!adminAuthService.isAdmin(chatId)) {
            return;
        }

        stageByChatId.put(chatId, AdminStage.AWAITING_BROADCAST_TEXT);
        telegramExecutor.sendMessage(chatId, localizationService.get(languageOf(chatId), "admin.broadcast.ask"));
    }

    public void handleBroadcastText(Update update) {
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        if (!adminAuthService.isAdmin(chatId)) {
            stageByChatId.remove(chatId);
            return;
        }

        Language language = languageOf(chatId);
        String text = HtmlEscaper.escape(message.getText().trim());
        stageByChatId.remove(chatId);
        pendingBroadcastByChatId.put(chatId, text);

        String confirmationText = localizationService.get(language, "admin.broadcast.confirm_prompt", text);
        telegramExecutor.sendMessageWithKeyboard(chatId, confirmationText,
                keyboardService.buildAdminBroadcastConfirmKeyboard(language));
    }

    public void handleBroadcastConfirmCallback(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        if (!adminAuthService.isAdmin(chatId)) {
            return;
        }

        Language language = languageOf(chatId);
        String text = pendingBroadcastByChatId.remove(chatId);
        if (text == null) {
            telegramExecutor.sendMessage(chatId, localizationService.get(language, "admin.broadcast.not_found"));
            return;
        }

        BroadcastResult result = adminService.broadcastMessage(text);
        telegramExecutor.sendMessage(chatId, localizationService.get(language, "admin.broadcast.result",
                result.sentCount(), result.totalRecipients(), result.failedCount()));
    }

    public void handleBroadcastCancelCallback(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        if (!adminAuthService.isAdmin(chatId)) {
            return;
        }

        Language language = languageOf(chatId);
        pendingBroadcastByChatId.remove(chatId);
        telegramExecutor.sendMessage(chatId, localizationService.get(language, "admin.broadcast.cancelled"));
        telegramExecutor.sendMessageWithKeyboard(chatId, localizationService.get(language, "admin.panel.title"),
                keyboardService.buildAdminMenuKeyboard(language));
    }

    public void handleSearchStartCallback(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        if (!adminAuthService.isAdmin(chatId)) {
            return;
        }

        stageByChatId.put(chatId, AdminStage.AWAITING_SEARCH_QUERY);
        telegramExecutor.sendMessage(chatId, localizationService.get(languageOf(chatId), "admin.search.ask"));
    }

    public void handleSearchText(Update update) {
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        if (!adminAuthService.isAdmin(chatId)) {
            stageByChatId.remove(chatId);
            return;
        }

        Language language = languageOf(chatId);
        stageByChatId.remove(chatId);
        List<UserSummaryProjection> results = adminService.searchUsers(message.getText().trim());
        if (results.isEmpty()) {
            telegramExecutor.sendMessage(chatId, localizationService.get(language, "admin.search.empty"));
            return;
        }

        StringBuilder text = new StringBuilder(localizationService.get(language, "admin.search.title")).append("\n\n");
        for (UserSummaryProjection user : results) {
            text.append(formatUserSummary(user, language)).append("\n");
        }
        telegramExecutor.sendMessage(chatId, text.toString());
    }

    public void handleErrorsCallback(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();
        if (!adminAuthService.isAdmin(chatId)) {
            return;
        }

        Language language = languageOf(chatId);
        Integer messageId = callbackQuery.getMessage().getMessageId();
        List<ErrorLogEntity> errors = adminService.getRecentErrors(RECENT_ERRORS_LIMIT);
        telegramExecutor.editMessageText(chatId, messageId, formatErrors(errors, language));
        telegramExecutor.editMessageReplyMarkup(chatId, messageId, keyboardService.buildAdminMenuKeyboard(language));
    }

    private String formatStats(SystemStats stats, Language language) {
        return localizationService.get(language, "admin.stats.block", stats.totalUsers(), stats.newUsersToday(),
                stats.activeUsersToday(), stats.totalActiveGoals(), stats.tasksDoneToday(), stats.errorsLast24h());
    }

    private String formatUserSummary(UserSummaryProjection user, Language language) {
        String name = user.getName() != null
                ? HtmlEscaper.escape(user.getName())
                : localizationService.get(language, "admin.user_no_name");
        return "%s — %s — %s — %s".formatted(name, user.getTelegramId(), user.getTonePreference(),
                TimeUtil.formatDateOnly(user.getCreatedAt()));
    }

    private String formatErrors(List<ErrorLogEntity> errors, Language language) {
        if (errors.isEmpty()) {
            return localizationService.get(language, "admin.errors.empty");
        }

        StringBuilder text = new StringBuilder(localizationService.get(language, "admin.errors.title")).append("\n\n");
        for (ErrorLogEntity error : errors) {
            text.append("⚠️ ").append(error.getSourceModule()).append(" — ").append(error.getExceptionType()).append("\n")
                    .append(HtmlEscaper.escape(truncate(error.getMessage(), ERROR_MESSAGE_PREVIEW_LENGTH))).append("\n")
                    .append(TimeUtil.formatForDisplay(error.getCreatedAt())).append("\n\n");
        }
        return text.toString().trim();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 1) + "…";
    }

    private Language languageOf(Long chatId) {
        return userService.findByTelegramId(chatId).map(User::getLanguage).orElse(Language.UZ);
    }

    private enum AdminStage {
        AWAITING_BROADCAST_TEXT,
        AWAITING_SEARCH_QUERY
    }
}
