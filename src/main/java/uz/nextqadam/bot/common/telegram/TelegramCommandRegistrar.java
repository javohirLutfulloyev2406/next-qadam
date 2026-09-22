package uz.nextqadam.bot.common.telegram;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import jakarta.annotation.PostConstruct;
import uz.nextqadam.bot.common.LocalizationService;
import uz.nextqadam.bot.common.enums.Language;

@Component
public class TelegramCommandRegistrar {

    private static final Logger log = LoggerFactory.getLogger(TelegramCommandRegistrar.class);

    // Hozircha faqat UpdateDispatcher'da real handler'ga ulangan komandalar ro'yxatga olinadi.
    private static final List<uz.nextqadam.bot.common.BotCommand> REGISTERED_COMMANDS = List.of(
            uz.nextqadam.bot.common.BotCommand.START,
            uz.nextqadam.bot.common.BotCommand.HELP,
            uz.nextqadam.bot.common.BotCommand.NEW_GOAL,
            uz.nextqadam.bot.common.BotCommand.NEXT_STEP,
            uz.nextqadam.bot.common.BotCommand.DONE,
            uz.nextqadam.bot.common.BotCommand.GOALS,
            uz.nextqadam.bot.common.BotCommand.PROFILE,
            uz.nextqadam.bot.common.BotCommand.MEMORY,
            uz.nextqadam.bot.common.BotCommand.FORGET,
            uz.nextqadam.bot.common.BotCommand.PLAN_DAY,
            uz.nextqadam.bot.common.BotCommand.BRAIN_DUMP,
            uz.nextqadam.bot.common.BotCommand.IDEAS,
            uz.nextqadam.bot.common.BotCommand.MOTIVATE,
            uz.nextqadam.bot.common.BotCommand.SOS,
            uz.nextqadam.bot.common.BotCommand.EVENING_CHECKIN,
            uz.nextqadam.bot.common.BotCommand.RESET_ACCOUNT,
            uz.nextqadam.bot.common.BotCommand.LANGUAGE
            // TEST_RETRO, TEST_DRIFT, TEST_NUDGE va ADMIN ataylab qo'shilmagan — bular faqat
            // dasturchi/administrator uchun "yashirin" komandalar, foydalanuvchiga "/" menyusida
            // ko'rinmasligi kerak.
    );

    // REGISTERED_COMMANDS'dagi har bir komandaning tavsifi uchun i18n kaliti — BotCommand.getDescription()
    // faqat o'zbekcha (default) matnni saqlaydi, shu sababli Telegram'ning o'z (mijoz tili bo'yicha)
    // komandalar ro'yxati uchun uch tilda alohida tavsif shu xarita orqali olinadi.
    private static final Map<uz.nextqadam.bot.common.BotCommand, String> DESCRIPTION_KEYS = Map.ofEntries(
            Map.entry(uz.nextqadam.bot.common.BotCommand.START, "command.start"),
            Map.entry(uz.nextqadam.bot.common.BotCommand.HELP, "command.help"),
            Map.entry(uz.nextqadam.bot.common.BotCommand.NEW_GOAL, "command.newgoal"),
            Map.entry(uz.nextqadam.bot.common.BotCommand.NEXT_STEP, "command.nextstep"),
            Map.entry(uz.nextqadam.bot.common.BotCommand.DONE, "command.done"),
            Map.entry(uz.nextqadam.bot.common.BotCommand.GOALS, "command.goals"),
            Map.entry(uz.nextqadam.bot.common.BotCommand.PROFILE, "command.profile"),
            Map.entry(uz.nextqadam.bot.common.BotCommand.MEMORY, "command.memory"),
            Map.entry(uz.nextqadam.bot.common.BotCommand.FORGET, "command.forget"),
            Map.entry(uz.nextqadam.bot.common.BotCommand.PLAN_DAY, "command.planday"),
            Map.entry(uz.nextqadam.bot.common.BotCommand.BRAIN_DUMP, "command.braindump"),
            Map.entry(uz.nextqadam.bot.common.BotCommand.IDEAS, "command.ideas"),
            Map.entry(uz.nextqadam.bot.common.BotCommand.MOTIVATE, "command.motivate"),
            Map.entry(uz.nextqadam.bot.common.BotCommand.SOS, "command.sos"),
            Map.entry(uz.nextqadam.bot.common.BotCommand.EVENING_CHECKIN, "command.kunim"),
            Map.entry(uz.nextqadam.bot.common.BotCommand.RESET_ACCOUNT, "command.reset"),
            Map.entry(uz.nextqadam.bot.common.BotCommand.LANGUAGE, "command.language")
    );

    private final TelegramBotFacade telegramBotFacade;
    private final LocalizationService localizationService;

    public TelegramCommandRegistrar(TelegramBotFacade telegramBotFacade, LocalizationService localizationService) {
        this.telegramBotFacade = telegramBotFacade;
        this.localizationService = localizationService;
    }

    @PostConstruct
    public void registerCommands() {
        // Telegram mijozining o'z tiliga (foydalanuvchi bot ichida tanlagan tildan mustaqil) mos
        // komandalar ro'yxati ko'rsatilishi uchun setMyCommands har bir Language uchun alohida,
        // languageCode bilan chaqiriladi.
        for (Language language : Language.values()) {
            registerCommandsForLanguage(language);
        }
    }

    private void registerCommandsForLanguage(Language language) {
        List<BotCommand> commands = REGISTERED_COMMANDS.stream()
                .map(command -> BotCommand.builder()
                        .command(command.getCommand().substring(1))
                        .description(localizationService.get(language, DESCRIPTION_KEYS.get(command)))
                        .build())
                .toList();

        try {
            telegramBotFacade.execute(SetMyCommands.builder()
                    .commands(commands)
                    .languageCode(language.getTelegramLocale())
                    .build());
        } catch (TelegramApiException e) {
            log.error("Bot komandalarini (setMyCommands) ro'yxatdan o'tkazishda xatolik yuz berdi. til={}", language, e);
        }
    }
}
