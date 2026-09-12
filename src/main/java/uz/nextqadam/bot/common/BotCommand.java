package uz.nextqadam.bot.common;

import java.util.Arrays;
import java.util.Optional;

public enum BotCommand {

    START("/start", "Botni ishga tushirish"),
    HELP("/help", "Yordam va buyruqlar ro'yxati"),
    PROFILE("/profile", "Foydalanuvchi profilini ko'rish"),
    MOTIVATE("/motivate", "Motivatsion xabar olish"),
    SOS("/sos", "Shoshilinch yordam so'rash"),
    FORGET("/forget", "Xotiradagi ma'lumotni o'chirish"),
    MEMORY("/memory", "Xotiradagi ma'lumotlarni ko'rish");

    private final String command;
    private final String description;

    BotCommand(String command, String description) {
        this.command = command;
        this.description = description;
    }

    public String getCommand() {
        return command;
    }

    public String getDescription() {
        return description;
    }

    public static Optional<BotCommand> fromText(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String normalized = text.trim().split("\\s+")[0].split("@")[0].toLowerCase();
        return Arrays.stream(values())
                .filter(command -> command.command.equalsIgnoreCase(normalized))
                .findFirst();
    }
}
