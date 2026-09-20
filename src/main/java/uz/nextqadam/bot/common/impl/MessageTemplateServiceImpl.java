package uz.nextqadam.bot.common.impl;

import java.util.Map;

import org.springframework.stereotype.Service;

import uz.nextqadam.bot.common.MessageTemplateService;
import uz.nextqadam.bot.common.enums.ToneType;

@Service
public class MessageTemplateServiceImpl implements MessageTemplateService {

    private static final Map<ToneType, String> WELCOME_AFTER_TONE = Map.of(
            ToneType.SOFT, "Rahmat, %s! 🌱 Endi tanishganimizga ishonaman — birga, shoshilmasdan, "
                    + "o'z sur'atingizda harakatlanamiz. Birinchi maqsadingizni qo'yishga tayyor bo'lsangiz, "
                    + "quyidagi tugmadan foydalaning. 💛",
            ToneType.NORMAL, "Tanlov saqlandi, %s. 📋 Endi birinchi maqsadingizni belgilashdan boshlaymiz — "
                    + "quyidagi tugmalar orqali harakatlaning.",
            ToneType.HARD, "%s, tanlov qabul qilindi. 🔥 Gapni ko'paytirmaymiz — hoziroq birinchi maqsadingizni "
                    + "belgilang va ishga tushamiz.",
            ToneType.HARDCORE, "Tayyor, %s. ⚡ Bahonalarga o'rin yo'q — hozir maqsad qo'y va birinchi qadamni "
                    + "tashla. Kutish vaqt yo'qotishdir."
    );

    private static final Map<ToneType, String> TASK_DONE_CONGRATS = Map.of(
            ToneType.SOFT, "Zo'r ish qildingiz 🙂 '%s' bajarildi. O'zingizga vaqt ajratganingiz uchun rahmat, "
                    + "davom etamiz asta-sekin.",
            ToneType.NORMAL, "✅ '%s' bajarildi. Davom etamiz.",
            ToneType.HARD, "'%s' — tayyor. Keyingisiga o'tamiz, to'xtamang.",
            ToneType.HARDCORE, "'%s' bajarildi. Kayfiyatni kutma — navbatdagisi kutib turmaydi."
    );

    private static final Map<ToneType, String> NO_PENDING_TASK = Map.of(
            ToneType.SOFT, "Hozircha bajarilishi kerak bo'lgan vazifa yo'q ekan 🙂 Xohlasangiz, /newgoal orqali "
                    + "yangi maqsad qo'shishingiz mumkin — shoshilmang, o'z vaqtida boshlaymiz.",
            ToneType.NORMAL, "Hozircha faol vazifa yo'q. /newgoal orqali yangi maqsad qo'shing yoki keyinroq "
                    + "qayta tekshiring.",
            ToneType.HARD, "Faol vazifa yo'q. Vaqtni behuda o'tkazmang — /newgoal orqali yangi maqsad qo'ying.",
            ToneType.HARDCORE, "Vazifa yo'q — demak sabab yo'q. /newgoal orqali hoziroq maqsad qo'y, bo'sh "
                    + "turish variant emas."
    );

    private static final Map<ToneType, String> GOAL_DECOMPOSITION_INTRO = Map.of(
            ToneType.SOFT, "Ajoyib maqsad! 🌱 Buni siz uchun kichik, bosqichma-bosqich qadamlarga bo'lib "
                    + "chiqdim — birga, shoshilmasdan boradi:",
            ToneType.NORMAL, "🎯 Maqsadingiz bosqichlarga bo'lindi:",
            ToneType.HARD, "Maqsad qabul qilindi. Bosqichlar tayyor — endi harakat vaqti:",
            ToneType.HARDCORE, "Rejani tuzdim. Gap yo'q, endi bajarish bosqichi — orqaga qarash yo'q:"
    );

    private static final Map<ToneType, String> REMINDER_NUDGE = Map.of(
            ToneType.SOFT, "Xayrli kun 🌱 Sizga eslatib qo'yay — \"%s\" hali kutmoqda. Shoshilmasdan, "
                    + "imkoningiz bo'lganda qarab chiqing.",
            ToneType.NORMAL, "📌 Eslatma: \"%s\" hali bajarilmagan.",
            ToneType.HARD, "\"%s\" — hali qilinmadi. Vaqt ketyapti.",
            ToneType.HARDCORE, "\"%s\". Hali qo'l tegmagan. Boshqa bahona yo'q — hoziroq bosh."
    );

    private static final Map<ToneType, String> SNOOZE_ACK = Map.of(
            ToneType.SOFT, "Hechqisi yo'q, hademay qayta eslataman 🙂",
            ToneType.NORMAL, "⏰ Xo'p, bir soatdan keyin qayta eslataman.",
            ToneType.HARD, "Yaxshi, bir soatdan keyin yana eslataman. Lekin cho'zmang.",
            ToneType.HARDCORE, "Bir soat berdim. Undan ortiq kutish yo'q."
    );

    private static final Map<ToneType, String> ADAPTIVE_SHRINK_NOTICE = Map.of(
            ToneType.SOFT, "Sezdim, bu vazifa biroz og'irroq ekan 🌱 Uni %d daqiqagacha kichraytirdim — "
                    + "endi bemalol boshlashingiz mumkin.",
            ToneType.NORMAL, "🔄 Bu vazifani 3 marta kechiktirdingiz — men uni %d daqiqagacha kichraytirdim. "
                    + "Endi osonroq bo'ladi 🙂",
            ToneType.HARD, "3 marta kechiktirdingiz. Vazifani %d daqiqaga tushirdim — endi bahona qolmadi.",
            ToneType.HARDCORE, "Uch marta qochding. Endi bahona qolmadi — bor-yo'g'i %d daqiqa."
    );

    private static final Map<ToneType, String> TYPING_PLACEHOLDER = Map.of(
            ToneType.SOFT, "🌱 O'ylab ko'ryapman, biroz kuting...",
            ToneType.NORMAL, "⏳ Tayyorlanmoqda...",
            ToneType.HARD, "⚙️ Ishlov berilmoqda.",
            ToneType.HARDCORE, "🔥 Kutib tur, hozir bo'ladi."
    );

    @Override
    public String welcomeAfterTone(ToneType tone, String userName) {
        return WELCOME_AFTER_TONE.get(tone).formatted(userName);
    }

    @Override
    public String taskDoneCongrats(ToneType tone, String taskTitle) {
        return TASK_DONE_CONGRATS.get(tone).formatted(taskTitle);
    }

    @Override
    public String noPendingTask(ToneType tone) {
        return NO_PENDING_TASK.get(tone);
    }

    @Override
    public String goalDecompositionIntro(ToneType tone) {
        return GOAL_DECOMPOSITION_INTRO.get(tone);
    }

    @Override
    public String reminderNudge(ToneType tone, String taskTitle) {
        return REMINDER_NUDGE.get(tone).formatted(taskTitle);
    }

    @Override
    public String snoozeAck(ToneType tone) {
        return SNOOZE_ACK.get(tone);
    }

    @Override
    public String adaptiveShrinkNotice(ToneType tone, int newEstimatedMinutes) {
        return ADAPTIVE_SHRINK_NOTICE.get(tone).formatted(newEstimatedMinutes);
    }

    @Override
    public String typingPlaceholder(ToneType tone) {
        return TYPING_PLACEHOLDER.get(tone);
    }
}
