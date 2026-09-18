package uz.nextqadam.bot.common;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

public enum BotCommand {

    START("/start", "Botni ishga tushirish"),
    HELP("/help", "Yordam va buyruqlar ro'yxati"),
    PROFILE("/profile", "Foydalanuvchi profilini ko'rish"),
    MOTIVATE("/motivate", "Motivatsion xabar olish"),
    SOS("/sos", "Shoshilinch yordam so'rash"),
    FORGET("/forget", "Xotiradagi ma'lumotni o'chirish"),
    MEMORY("/memory", "Xotiradagi ma'lumotlarni ko'rish"),
    NEW_GOAL("/newgoal", "Yangi maqsad qo'shish"),
    NEXT_STEP("/nextstep", "Bugungi keyingi qadamni ko'rsatish"),
    DONE("/done", "Joriy vazifani bajarilgan deb belgilash"),
    GOALS("/goals", "Faol maqsadlarni ko'rish"),
    PLAN_DAY("/planday", "Bugungi ustuvor vazifalarni belgilash"),
    BRAIN_DUMP("/braindump", "Fikrlarni tez yozib tashlash"),
    IDEAS("/ideas", "Saqlangan g'oyalarni ko'rish");

    // Doimiy tugmalar panelidagi (ReplyKeyboard) matnlarni tegishli komandaga bog'lash uchun.
    private static final Map<String, BotCommand> BUTTON_LABEL_TO_COMMAND = Map.of(
            "🎯 Yangi maqsad", NEW_GOAL,
            "📌 Keyingi qadam", NEXT_STEP,
            "✅ Bajardim", DONE,
            "👤 Profil", PROFILE,
            "🌅 Kun rejasi", PLAN_DAY,
            "🧠 Fikr tashla", BRAIN_DUMP,
            "🔥 Motivatsiya", MOTIVATE,
            "🆘 Yordam kerak", SOS
    );

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

    public static Optional<BotCommand> fromButtonLabel(String label) {
        if (label == null || label.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(BUTTON_LABEL_TO_COMMAND.get(label.trim()));
    }
}
