package uz.nextqadam.bot.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

import uz.nextqadam.bot.common.keyboard.KeyboardService;
import uz.nextqadam.bot.common.keyboard.KeyboardService.MemoryListOption;
import uz.nextqadam.bot.common.telegram.TelegramExecutor;
import uz.nextqadam.bot.user.UserService;

@Component
public class MemoryHandler {

    private static final String DELETE_PREFIX = "MEMORY_DELETE_";
    private static final String DELETE_ALL_ASK = "MEMORY_DELETE_ALL_ASK";
    private static final String DELETE_ALL_CONFIRM = "MEMORY_DELETE_ALL_CONFIRM";
    private static final String DELETE_ALL_CANCEL = "MEMORY_DELETE_ALL_CANCEL";
    private static final String VIEW_CALLBACK = "MEMORY_VIEW";

    private static final int MEMORY_LIST_LIMIT = 10;
    private static final int VALUE_PREVIEW_MAX_LENGTH = 60;

    private final MemoryService memoryService;
    private final UserService userService;
    private final KeyboardService keyboardService;
    private final TelegramExecutor telegramExecutor;

    public MemoryHandler(MemoryService memoryService, UserService userService, KeyboardService keyboardService,
                          TelegramExecutor telegramExecutor) {
        this.memoryService = memoryService;
        this.userService = userService;
        this.keyboardService = keyboardService;
        this.telegramExecutor = telegramExecutor;
    }

    public boolean isMemoryView(String callbackData) {
        return VIEW_CALLBACK.equals(callbackData);
    }

    public boolean isMemoryDeleteAllAsk(String callbackData) {
        return DELETE_ALL_ASK.equals(callbackData);
    }

    public boolean isMemoryDeleteAllConfirm(String callbackData) {
        return DELETE_ALL_CONFIRM.equals(callbackData);
    }

    public boolean isMemoryDeleteAllCancel(String callbackData) {
        return DELETE_ALL_CANCEL.equals(callbackData);
    }

    public boolean isMemoryDeleteOne(String callbackData) {
        return callbackData != null
                && callbackData.startsWith(DELETE_PREFIX)
                && !callbackData.startsWith(DELETE_PREFIX + "ALL_");
    }

    public void handleMemoryCommand(Update update) {
        showMemoryList(update.getMessage().getChatId());
    }

    public void handleForgetCommand(Update update) {
        askDeleteAllConfirmation(update.getMessage().getChatId());
    }

    public void handleMemoryViewCallback(Update update) {
        showMemoryList(update.getCallbackQuery().getMessage().getChatId());
    }

    public void handleDeleteAllAskCallback(Update update) {
        askDeleteAllConfirmation(update.getCallbackQuery().getMessage().getChatId());
    }

    public void handleDeleteOneCallback(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();
        UUID memoryItemId = UUID.fromString(callbackQuery.getData().substring(DELETE_PREFIX.length()));

        userService.findByTelegramId(chatId).ifPresent(user -> {
            memoryService.forgetOne(user.getId(), memoryItemId);
            telegramExecutor.sendMessage(chatId, "🗑️ O'chirildi.");
        });
        showMemoryList(chatId);
    }

    public void handleDeleteAllConfirmCallback(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        userService.findByTelegramId(chatId).ifPresent(user -> memoryService.forgetAll(user.getId()));
        telegramExecutor.sendMessage(chatId, "✅ Xotiram tozalandi. Yangi sahifadan boshlaymiz 🙂");
    }

    public void handleDeleteAllCancelCallback(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        telegramExecutor.sendMessage(chatId, "Bekor qilindi, hech narsa o'chirilmadi.");
        showMemoryList(chatId);
    }

    private void askDeleteAllConfirmation(Long chatId) {
        telegramExecutor.sendMessageWithKeyboard(chatId,
                "⚠️ Butun xotiramni o'chiray deysizmi? Bu amalni qaytarib bo'lmaydi.",
                keyboardService.buildConfirmDeleteAllKeyboard());
    }

    private void showMemoryList(Long chatId) {
        userService.findByTelegramId(chatId).ifPresentOrElse(
                user -> {
                    List<MemoryItem> items = memoryService.getTopMemories(user.getId(), MEMORY_LIST_LIMIT);
                    if (items.isEmpty()) {
                        telegramExecutor.sendMessage(chatId,
                                "🧠 Hozircha hech narsa saqlamagan ekanman. Maqsad qo'shsangiz, men muhim "
                                        + "narsalarni eslab qolaman.");
                        return;
                    }

                    StringBuilder text = new StringBuilder("🧠 <b>Men bular haqida bilaman:</b>\n\n");
                    List<MemoryListOption> options = new ArrayList<>();
                    for (MemoryItem item : items) {
                        text.append("• ").append(item.getKey()).append(": ")
                                .append(truncate(item.getValue(), VALUE_PREVIEW_MAX_LENGTH)).append("\n");
                        options.add(new MemoryListOption(item.getId(), item.getKey()));
                    }

                    telegramExecutor.sendMessageWithKeyboard(chatId, text.toString(),
                            keyboardService.buildMemoryListKeyboard(options));
                },
                () -> telegramExecutor.sendMessage(chatId, "Avval /start orqali ro'yxatdan o'ting.")
        );
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 1) + "…";
    }
}
