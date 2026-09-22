package uz.nextqadam.bot.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

import uz.nextqadam.bot.common.LocalizationService;
import uz.nextqadam.bot.common.enums.Language;
import uz.nextqadam.bot.common.keyboard.KeyboardService;
import uz.nextqadam.bot.common.keyboard.KeyboardService.MemoryListOption;
import uz.nextqadam.bot.common.telegram.TelegramExecutor;
import uz.nextqadam.bot.user.User;
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
    private final LocalizationService localizationService;

    public MemoryHandler(MemoryService memoryService, UserService userService, KeyboardService keyboardService,
                          TelegramExecutor telegramExecutor, LocalizationService localizationService) {
        this.memoryService = memoryService;
        this.userService = userService;
        this.keyboardService = keyboardService;
        this.telegramExecutor = telegramExecutor;
        this.localizationService = localizationService;
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
            telegramExecutor.sendMessage(chatId, localizationService.get(user.getLanguage(), "memory.item.deleted"));
        });
        showMemoryList(chatId);
    }

    public void handleDeleteAllConfirmCallback(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Language language = userService.findByTelegramId(chatId)
                .map(user -> {
                    memoryService.forgetAll(user.getId());
                    return user.getLanguage();
                })
                .orElse(Language.UZ);
        telegramExecutor.sendMessage(chatId, localizationService.get(language, "memory.deleteAll.done"));
    }

    public void handleDeleteAllCancelCallback(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Language language = userService.findByTelegramId(chatId).map(User::getLanguage).orElse(Language.UZ);
        telegramExecutor.sendMessage(chatId, localizationService.get(language, "memory.deleteAll.cancelled"));
        showMemoryList(chatId);
    }

    private void askDeleteAllConfirmation(Long chatId) {
        Language language = userService.findByTelegramId(chatId).map(User::getLanguage).orElse(Language.UZ);
        telegramExecutor.sendMessageWithKeyboard(chatId,
                localizationService.get(language, "memory.confirm.deleteAll"),
                keyboardService.buildConfirmDeleteAllKeyboard(language));
    }

    private void showMemoryList(Long chatId) {
        userService.findByTelegramId(chatId).ifPresentOrElse(
                user -> {
                    Language language = user.getLanguage();
                    List<MemoryItem> items = memoryService.getTopMemories(user.getId(), MEMORY_LIST_LIMIT);
                    if (items.isEmpty()) {
                        telegramExecutor.sendMessage(chatId, localizationService.get(language, "memory.list.empty"));
                        return;
                    }

                    StringBuilder text = new StringBuilder(localizationService.get(language, "memory.list.header"));
                    List<MemoryListOption> options = new ArrayList<>();
                    for (MemoryItem item : items) {
                        text.append("• ").append(item.getKey()).append(": ")
                                .append(truncate(item.getValue(), VALUE_PREVIEW_MAX_LENGTH)).append("\n");
                        options.add(new MemoryListOption(item.getId(), item.getKey()));
                    }

                    telegramExecutor.sendMessageWithKeyboard(chatId, text.toString(),
                            keyboardService.buildMemoryListKeyboard(options, language));
                },
                () -> telegramExecutor.sendMessage(chatId, localizationService.get(Language.UZ, "common.please_start"))
        );
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 1) + "…";
    }
}
