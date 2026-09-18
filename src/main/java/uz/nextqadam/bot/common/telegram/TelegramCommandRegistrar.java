package uz.nextqadam.bot.common.telegram;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import jakarta.annotation.PostConstruct;

@Component
public class TelegramCommandRegistrar {

    private static final Logger log = LoggerFactory.getLogger(TelegramCommandRegistrar.class);

    // Hozircha faqat UpdateDispatcher'da real handler'ga ulangan komandalar ro'yxatga olinadi —
    // help/motivate/sos hali TODO, ular ro'yxatga qo'shilsa foydalanuvchi ularni "/" menyusida
    // ko'rib bosadi, lekin botdan hozircha hech qanday javob kelmaydi.
    private static final List<uz.nextqadam.bot.common.BotCommand> REGISTERED_COMMANDS = List.of(
            uz.nextqadam.bot.common.BotCommand.START,
            uz.nextqadam.bot.common.BotCommand.NEW_GOAL,
            uz.nextqadam.bot.common.BotCommand.NEXT_STEP,
            uz.nextqadam.bot.common.BotCommand.DONE,
            uz.nextqadam.bot.common.BotCommand.PROFILE,
            uz.nextqadam.bot.common.BotCommand.MEMORY,
            uz.nextqadam.bot.common.BotCommand.FORGET
    );

    private final TelegramBotFacade telegramBotFacade;

    public TelegramCommandRegistrar(TelegramBotFacade telegramBotFacade) {
        this.telegramBotFacade = telegramBotFacade;
    }

    @PostConstruct
    public void registerCommands() {
        List<BotCommand> commands = REGISTERED_COMMANDS.stream()
                .map(command -> BotCommand.builder()
                        .command(command.getCommand().substring(1))
                        .description(command.getDescription())
                        .build())
                .toList();

        try {
            telegramBotFacade.execute(SetMyCommands.builder().commands(commands).build());
        } catch (TelegramApiException e) {
            log.error("Bot komandalarini (setMyCommands) ro'yxatdan o'tkazishda xatolik yuz berdi", e);
        }
    }
}
