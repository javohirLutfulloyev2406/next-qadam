package uz.nextqadam.bot.common;

import java.util.Arrays;
import java.util.Optional;

public enum BotCommand {

    START("/start", "Botni ishga tushirish", null),
    HELP("/help", "Botdan foydalanish qo'llanmasi", null),
    PROFILE("/profile", "Foydalanuvchi profilini ko'rish", "menu.profile"),
    MOTIVATE("/motivate", "Motivatsion xabar olish", "menu.motivate"),
    SOS("/sos", "Shoshilinch yordam so'rash", "menu.sos"),
    FORGET("/forget", "Xotiradagi ma'lumotni o'chirish", null),
    MEMORY("/memory", "Xotiradagi ma'lumotlarni ko'rish", null),
    NEW_GOAL("/newgoal", "Yangi maqsad qo'shish", "menu.newgoal"),
    NEXT_STEP("/nextstep", "Bugungi keyingi qadamni ko'rsatish", "menu.nextstep"),
    DONE("/done", "Joriy vazifani bajarilgan deb belgilash", "menu.done"),
    GOALS("/goals", "Faol maqsadlarni ko'rish", null),
    PLAN_DAY("/planday", "Bugungi ustuvor vazifalarni belgilash", "menu.planday"),
    BRAIN_DUMP("/braindump", "Fikrlarni tez yozib tashlash", "menu.braindump"),
    IDEAS("/ideas", "Saqlangan g'oyalarni ko'rish", null),
    EVENING_CHECKIN("/kunim", "Kechki hisobot berish", "menu.eveningcheckin"),
    RESET_ACCOUNT("/reset", "Hisobni tozalash yoki butunlay o'chirish", null),
    LANGUAGE("/language", "Tilni o'zgartirish", null),
    // Dasturchi uchun "yashirin" komandalar — asosiy menyuga va setMyCommands ro'yxatiga QO'SHILMAYDI.
    TEST_RETRO("/testretro", "Haftalik retrospektivani darhol sinash", null),
    TEST_DRIFT("/testdrift", "Goal drift tekshiruvini darhol sinash", null),
    TEST_NUDGE("/testnudge", "Kunlik ustuvorlik nudge'ini darhol sinash", null),
    // Faqat administrator uchun — description ataylab null, chunki setMyCommands ro'yxatiga
    // QO'SHILMAYDI (Telegram'ning "/" menyusida ko'rinmasligi kerak).
    ADMIN("/admin", null, null);

    private final String command;
    private final String description;
    // ReplyKeyboard'dagi doimiy tugma matniga mos i18n kaliti (masalan "menu.newgoal") — shu tugmani
    // ko'rsatadigan komandalar uchungina to'ldirilgan, qolganlari uchun null. ButtonLabelResolver bu
    // kalitni barcha Language'lar bo'yicha oldindan hisoblab, matndan komandaga teskari xarita quradi.
    private final String menuKey;

    BotCommand(String command, String description, String menuKey) {
        this.command = command;
        this.description = description;
        this.menuKey = menuKey;
    }

    public String getCommand() {
        return command;
    }

    public String getDescription() {
        return description;
    }

    public String getMenuKey() {
        return menuKey;
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
