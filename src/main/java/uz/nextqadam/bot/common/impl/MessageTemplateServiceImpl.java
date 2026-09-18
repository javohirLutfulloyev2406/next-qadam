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
}
