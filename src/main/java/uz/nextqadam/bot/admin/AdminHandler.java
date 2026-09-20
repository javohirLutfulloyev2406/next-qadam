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
import uz.nextqadam.bot.common.errorlog.ErrorLogEntity;
import uz.nextqadam.bot.common.keyboard.KeyboardService;
import uz.nextqadam.bot.common.telegram.TelegramExecutor;
import uz.nextqadam.bot.common.util.TimeUtil;
import uz.nextqadam.bot.user.UserRepository.UserSummaryProjection;

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
    private final KeyboardService keyboardService;
    private final TelegramExecutor telegramExecutor;

    public AdminHandler(AdminAuthService adminAuthService, AdminService adminService,
                         KeyboardService keyboardService, TelegramExecutor telegramExecutor) {
        this.adminAuthService = adminAuthService;
        this.adminService = adminService;
        this.keyboardService = keyboardService;
        this.telegramExecutor = telegramExecutor;
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
        // Admin bo'lmagan foydalanuvchiga botning o'zi buyruqni "tanimayapti"dek ko'rsatiladi —
        // bunday buyruq borligini oshkor qilmaslik uchun log ham yozilmaydi.
        if (!adminAuthService.isAdmin(chatId)) {
            telegramExecutor.sendMessage(chatId, "Bunday buyruq yo'q.");
            return;
        }
        telegramExecutor.sendMessageWithKeyboard(chatId, "🛠 <b>Admin panel</b>", keyboardService.buildAdminMenuKeyboard());
    }

    public void handleStatsCallback(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();
        if (!adminAuthService.isAdmin(chatId)) {
            return;
        }

        Integer messageId = callbackQuery.getMessage().getMessageId();
        SystemStats stats = adminService.getSystemStats();
        telegramExecutor.editMessageText(chatId, messageId, formatStats(stats));
        telegramExecutor.editMessageReplyMarkup(chatId, messageId, keyboardService.buildAdminMenuKeyboard());
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
        telegramExecutor.sendMessage(chatId, "📢 Barcha foydalanuvchiga yuboriladigan xabar matnini yozing:");
    }

    public void handleBroadcastText(Update update) {
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        if (!adminAuthService.isAdmin(chatId)) {
            stageByChatId.remove(chatId);
            return;
        }

        String text = HtmlEscaper.escape(message.getText().trim());
        stageByChatId.remove(chatId);
        pendingBroadcastByChatId.put(chatId, text);

        String confirmationText = "📢 Quyidagi xabar BARCHA foydalanuvchilarga yuboriladi:\n\n———\n" + text
                + "\n———\n\nTasdiqlaysizmi?";
        telegramExecutor.sendMessageWithKeyboard(chatId, confirmationText,
                keyboardService.buildAdminBroadcastConfirmKeyboard());
    }

    public void handleBroadcastConfirmCallback(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        if (!adminAuthService.isAdmin(chatId)) {
            return;
        }

        String text = pendingBroadcastByChatId.remove(chatId);
        if (text == null) {
            telegramExecutor.sendMessage(chatId, "Yuboriladigan xabar topilmadi, qaytadan urinib ko'ring.");
            return;
        }

        BroadcastResult result = adminService.broadcastMessage(text);
        telegramExecutor.sendMessage(chatId, "✅ Yuborildi: " + result.sentCount() + "/" + result.totalRecipients()
                + "\n❌ Yetkazilmadi: " + result.failedCount());
    }

    public void handleBroadcastCancelCallback(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        if (!adminAuthService.isAdmin(chatId)) {
            return;
        }

        pendingBroadcastByChatId.remove(chatId);
        telegramExecutor.sendMessage(chatId, "Bekor qilindi.");
        telegramExecutor.sendMessageWithKeyboard(chatId, "🛠 <b>Admin panel</b>", keyboardService.buildAdminMenuKeyboard());
    }

    public void handleSearchStartCallback(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        if (!adminAuthService.isAdmin(chatId)) {
            return;
        }

        stageByChatId.put(chatId, AdminStage.AWAITING_SEARCH_QUERY);
        telegramExecutor.sendMessage(chatId, "🔍 Ism yoki Telegram ID bo'yicha qidiruv so'zini yozing:");
    }

    public void handleSearchText(Update update) {
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        if (!adminAuthService.isAdmin(chatId)) {
            stageByChatId.remove(chatId);
            return;
        }

        stageByChatId.remove(chatId);
        List<UserSummaryProjection> results = adminService.searchUsers(message.getText().trim());
        if (results.isEmpty()) {
            telegramExecutor.sendMessage(chatId, "Hech narsa topilmadi.");
            return;
        }

        StringBuilder text = new StringBuilder("🔍 <b>Qidiruv natijalari:</b>\n\n");
        for (UserSummaryProjection user : results) {
            text.append(formatUserSummary(user)).append("\n");
        }
        telegramExecutor.sendMessage(chatId, text.toString());
    }

    public void handleErrorsCallback(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();
        if (!adminAuthService.isAdmin(chatId)) {
            return;
        }

        Integer messageId = callbackQuery.getMessage().getMessageId();
        List<ErrorLogEntity> errors = adminService.getRecentErrors(RECENT_ERRORS_LIMIT);
        telegramExecutor.editMessageText(chatId, messageId, formatErrors(errors));
        telegramExecutor.editMessageReplyMarkup(chatId, messageId, keyboardService.buildAdminMenuKeyboard());
    }

    private String formatStats(SystemStats stats) {
        return """
                📊 <b>Statistika</b>

                👥 Jami foydalanuvchilar: %d
                🆕 Bugun qo'shilgan: %d
                🔥 Bugun faol: %d
                🎯 Faol maqsadlar: %d
                ✅ Bugun bajarilgan vazifalar: %d
                ⚠️ So'nggi 24 soatda xatolar: %d"""
                .formatted(stats.totalUsers(), stats.newUsersToday(), stats.activeUsersToday(),
                        stats.totalActiveGoals(), stats.tasksDoneToday(), stats.errorsLast24h());
    }

    private String formatUserSummary(UserSummaryProjection user) {
        String name = user.getName() != null ? HtmlEscaper.escape(user.getName()) : "(ismsiz)";
        return "%s — %s — %s — %s".formatted(name, user.getTelegramId(), user.getTonePreference(),
                TimeUtil.formatDateOnly(user.getCreatedAt()));
    }

    private String formatErrors(List<ErrorLogEntity> errors) {
        if (errors.isEmpty()) {
            return "⚠️ <b>So'nggi xatolar</b>\n\nXatolar yo'q. 🎉";
        }

        StringBuilder text = new StringBuilder("⚠️ <b>So'nggi xatolar</b>\n\n");
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

    private enum AdminStage {
        AWAITING_BROADCAST_TEXT,
        AWAITING_SEARCH_QUERY
    }
}
