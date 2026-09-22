package uz.nextqadam.bot.ai.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import uz.nextqadam.bot.ai.PromptBuilder;
import uz.nextqadam.bot.ai.dto.TaskSummaryForPrompt;
import uz.nextqadam.bot.common.enums.Language;
import uz.nextqadam.bot.common.enums.ToneType;

@Component
public class PromptBuilderImpl implements PromptBuilder {

    private static final Map<Language, String> LANGUAGE_NAME = Map.of(
            Language.UZ, "o'zbek",
            Language.RU, "rus",
            Language.EN, "ingliz"
    );

    /**
     * Har bir promptning oxiriga qo'shiladigan qat'iy til ko'rsatmasi — ko'rsatma matnining o'zi
     * doim o'zbekcha (LLM buni har qanday tilda ham to'g'ri tushunadi), lekin nomlangan til
     * javobning FAQAT shu tilda bo'lishini talab qiladi.
     */
    private String languageInstruction(Language language) {
        return "\n\nMUHIM: Javobingizni FAQAT " + LANGUAGE_NAME.get(language) + " tilida yoz. Boshqa tilda yozma.";
    }

    /**
     * Goal decomposition uchun alohida ko'rsatma — JSON struktura kalitlari (title, milestones,
     * tasks, period, estimatedMinutes) INGLIZCHA, o'zgarishsiz qolishi kerak (dastur ichki formati,
     * AiResponseParser aynan shu kalitlarni parse qiladi), lekin JSON ICHIDAGI QIYMATLAR (masalan
     * milestone va task nomlari) ko'rsatilgan tilda bo'lishi shart.
     */
    private String goalDecompositionLanguageInstruction(Language language) {
        return "\n\nMUHIM: JSON struktura kalitlari (\"title\", \"milestones\", \"tasks\", \"period\", "
                + "\"estimatedMinutes\" va h.k.) INGLIZCHA, o'zgarishsiz qoladi — bu dastur ichki formati. LEKIN "
                + "JSON ICHIDAGI QIYMATLAR (masalan milestone va task nomlari) FAQAT " + LANGUAGE_NAME.get(language)
                + " tilida yozilishi shart.";
    }

    private static final Map<ToneType, String> FREE_CHAT_PERSONALITY = Map.of(
            ToneType.SOFT, "Sen mehribon, sabr-toqatli va tushunuvchan hamrohsan. Foydalanuvchini hech qachon "
                    + "qoralamaysan, uni iliqlik va qo'llab-quvvatlash bilan tinglaysan.",
            ToneType.NORMAL, "Sen do'stona, sodda va aniq gapiradigan yordamchisan. Ortiqcha his-hayajonsiz, "
                    + "amaliy va samimiy maslahat berasan.",
            ToneType.HARD, "Sen tik va talabchan murabbiysan. Bahonalarni yoqtirmaysan, to'g'ridan-to'g'ri va "
                    + "qisqa gapirasan, lekin foydalanuvchining natijaga erishishini chin dildan xohlaysan.",
            ToneType.HARDCORE, "Sen qattiqqo'l, lekin g'amxo'r murabbiysan. Bahonalarga toqat qilmaysan, lekin "
                    + "foydalanuvchining muvaffaqiyatini chin dildan xohlaysan."
    );

    private static final String TEMPLATE = """
            Sen NextQadam ismli shaxsiy rivojlanish yordamchisisan. Foydalanuvchi katta \
            maqsadini erkin matnda yozadi, sening vazifang uni amalga oshiriladigan \
            bosqichlarga (milestone) va har bir bosqich uchun aniq, kichik vazifalarga \
            (task) bo'lib berish.

            Foydalanuvchi maqsadi: "%s"

            QAT'IY QOIDALAR:
            1. Javobing FAQAT quyidagi JSON formatida bo'lishi kerak. Hech qanday qo'shimcha \
            matn, izoh yoki markdown fence (```) bo'lmasin — javobning birinchi belgisi "{" \
            va oxirgi belgisi "}" bo'lishi shart.
            2. "milestones" massivida kamida 3 ta, ko'pi bilan 10 ta element bo'lsin.
            3. Har bir milestone'da "tasks" massivida kamida 1 ta, ko'pi bilan 5 ta element bo'lsin.
            4. "period" maydoni faqat "MONTH" yoki "WEEK" qiymatlaridan birini olishi mumkin.
            5. "title" maydonlari qisqa va aniq bo'lsin (maqsad "title"i 80 belgidan oshmasin).
            6. "estimatedMinutes" — butun son, vazifani bajarish uchun taxminiy daqiqalar soni.
            7. Har bir task "title"i 8 so'zdan oshmasin — qisqa va lo'nda bo'lsin.

            JSON STRUKTURASI:
            {
              "title": "qisqa maqsad nomi (max 80 belgi)",
              "milestones": [
                {
                  "title": "milestone nomi",
                  "period": "MONTH",
                  "tasks": [
                    {"title": "task nomi", "estimatedMinutes": 30}
                  ]
                }
              ]
            }
            """;

    private static final String BRAIN_DUMP_TEMPLATE = """
            Sen NextQadam ismli shaxsiy rivojlanish yordamchisisan. Foydalanuvchi xayoliga kelgan turli \
            fikrlarni erkin, tartibsiz matn shaklida yozadi. Sening vazifang bu matnni uchta toifaga \
            ajratish:

            1. "tasks" — aniq bajariladigan, fe'l bilan boshlanadigan ish (masalan: "Hisobotni yubor", \
            "Kitob sotib ol").
            2. "ideas" — kelajakda o'ylab ko'rish kerak bo'lgan fikr yoki taklif, hozir bajarilishi shart \
            bo'lmagan narsa.
            3. "reminders" — vaqtga bog'liq eslatma (masalan "ertaga qo'ng'iroq qil", "kechqurun dori \
            ich"). Har bir eslatma uchun "whenHint" maydoniga matndan chiqargan vaqt ishorangni yoz \
            (masalan "ertaga", "kechqurun") — agar aniq vaqt ishorasi bo'lmasa, bo'sh string qoldir.

            Foydalanuvchi matni: "%s"

            QAT'IY QOIDALAR:
            1. Javobing FAQAT quyidagi JSON formatida bo'lishi kerak. Hech qanday qo'shimcha matn, izoh \
            yoki markdown fence (```) bo'lmasin — javobning birinchi belgisi "{" va oxirgi belgisi "}" \
            bo'lishi shart.
            2. Har bir massiv har doim mavjud bo'lishi kerak, lekin bo'sh bo'lishi mumkin (masalan hech \
            qanday g'oya topilmasa "ideas": []).
            3. Matnni ortiqcha talqin qilma — faqat matnda aniq aytilgan narsalarni ajrat.

            JSON STRUKTURASI:
            {
              "tasks": ["..."],
              "ideas": ["..."],
              "reminders": [{"content": "...", "whenHint": "..."}]
            }
            """;

    private static final String EVENING_CHECKIN_TEMPLATE = """
            Sen NextQadam ismli shaxsiy rivojlanish yordamchisisan. Foydalanuvchi kechqurun bugungi kunini \
            erkin matnda yozib bergan. Sening vazifang — quyidagi bugungi vazifalar ro'yxatidan qaysilari \
            bajarilgan, qaysilari bajarilmaganini shu matndan aniqlash.

            BUGUNGI VAZIFALAR RO'YXATI (id — sarlavha):
            %s

            Foydalanuvchi yozgan matn: "%s"

            QAT'IY QOIDALAR:
            1. Javobing FAQAT quyidagi JSON formatida bo'lishi kerak. Hech qanday qo'shimcha matn, izoh \
            yoki markdown fence (```) bo'lmasin.
            2. "classifications" massivida yuqoridagi ro'yxatdagi HAR BIR vazifa uchun bitta element \
            bo'lsin — birortasi ham tashlab ketilmasin.
            3. "taskId" MAJBURIY ravishda yuqorida berilgan ID'lardan biri bo'lishi shart — o'zingdan \
            yangi ID o'ylab topma, mavjud ID'ni AYNAN o'zgarishsiz qaytar.
            4. "done" — true agar matndan shu vazifa bajarilgani tushunilsa, aks holda false.
            5. "reason" — agar "done" false bo'lsa, matndan chiqargan qisqa sabab (masalan "vaqt \
            topolmadim"); agar "done" true bo'lsa, bo'sh string "".
            6. "overallMood" — foydalanuvchining matnidan sezilgan umumiy kayfiyatning qisqa (5-8 so'zlik) \
            tasviri.

            JSON STRUKTURASI:
            {
              "classifications": [
                {"taskId": "berilgan-id", "done": true, "reason": ""}
              ],
              "overallMood": "qisqa kayfiyat tasviri"
            }
            """;

    private static final String WEEKLY_RETRO_TEMPLATE = """
            Sen NextQadam ismli shaxsiy rivojlanish yordamchisisan. Foydalanuvchining shu haftalik \
            natijalari asosida qisqa, iliq ohangdagi xulosa va kelasi hafta uchun bitta aniq tavsiya yoz.

            Statistika: %d ta vazifadan %d tasi bajarilgan.
            Bajargan vazifalari: %s
            Bajarmagan vazifalari: %s

            QAT'IY QOIDALAR:
            1. Javobing FAQAT quyidagi JSON formatida bo'lishi kerak, qo'shimcha matn yoki markdown \
            fence bo'lmasin.
            2. "narrative" — 2-3 gaplik, iliq va haqiqiy natijalarga asoslangan xulosa.
            3. "recommendation" — kelasi hafta uchun bitta aniq, amaliy tavsiya (1 gap).

            JSON STRUKTURASI:
            {
              "narrative": "...",
              "recommendation": "..."
            }
            """;

    private static final String GOAL_DRIFT_TEMPLATE = """
            Sen NextQadam ismli shaxsiy rivojlanish yordamchisisan. Foydalanuvchining faol maqsadi bilan \
            oxirgi 14 kunda aslida nima bilan shug'ullanganini solishtirib, moslik darajasini bahola.

            Faol maqsad: "%s"
            Oxirgi 14 kunlik vazifalari: %s

            Vazifangiz: 0 dan 100 gacha moslik balli ber (100 — to'liq mos, 0 — butunlay chetlashgan) va \
            qisqa izoh yoz. %s

            QAT'IY QOIDALAR:
            1. Javobing FAQAT quyidagi JSON formatida bo'lishi kerak, qo'shimcha matn yoki markdown \
            fence bo'lmasin.
            2. "matchScore" — butun son, 0 dan 100 gacha.
            3. "explanation" — 1-2 gaplik qisqa izoh.

            JSON STRUKTURASI:
            {
              "matchScore": 85,
              "explanation": "..."
            }
            """;

    @Override
    public String buildGoalDecompositionPrompt(String goalDescription, String memoryContext, Language language) {
        String prompt = TEMPLATE.formatted(goalDescription);
        if (memoryContext != null && !memoryContext.isBlank()) {
            prompt += "\n\nFoydalanuvchi haqida ma'lum ma'lumotlar:\n" + memoryContext;
        }
        return prompt + goalDecompositionLanguageInstruction(language);
    }

    @Override
    public String buildBrainDumpPrompt(String rawText, Language language) {
        return BRAIN_DUMP_TEMPLATE.formatted(rawText) + languageInstruction(language);
    }

    @Override
    public String buildFreeChatSystemPrompt(ToneType tone, String memoryContext, String recentGoalsSummary,
                                             Language language) {
        StringBuilder sb = new StringBuilder();
        sb.append("Sen NextQadam ismli shaxsiy rivojlanish yordamchisisan. ")
                .append(FREE_CHAT_PERSONALITY.get(tone))
                .append("\n\nFoydalanuvchi sen bilan erkin suhbatlashyapti — savolga javob ber, fikr almash yoki "
                        + "shunchaki suhbatlash.\n");

        boolean hasMemory = memoryContext != null && !memoryContext.isBlank();
        boolean hasGoals = recentGoalsSummary != null && !recentGoalsSummary.isBlank();
        if (hasMemory || hasGoals) {
            sb.append("\nFoydalanuvchi haqida bilganlaringiz:\n");
            if (hasMemory) {
                sb.append(memoryContext).append("\n");
            }
            if (hasGoals) {
                sb.append("Faol maqsadlari: ").append(recentGoalsSummary).append("\n");
            }
        }

        sb.append("\nMUHIM: Javobing QISQA bo'lsin — 3-4 gapdan oshmasin, Telegram'da o'qilishi qulay bo'lishi "
                + "uchun. Markdown yoki HTML teglaridan foydalanma, oddiy matn yoz.");
        sb.append(languageInstruction(language));
        return sb.toString();
    }

    @Override
    public String buildMotivationPrompt(ToneType tone, String lastCompletedTask, String activeGoalTitle,
                                         int completedTaskCount, Language language) {
        StringBuilder sb = new StringBuilder();
        sb.append("Sen NextQadam ismli shaxsiy rivojlanish yordamchisisan. ")
                .append(FREE_CHAT_PERSONALITY.get(tone))
                .append("\n\nFoydalanuvchiga o'zining haqiqiy tarixiga asoslangan, umumiy (generic) bo'lmagan, "
                        + "qisqa motivatsion xabar yoz.\n\n");

        boolean hasCompletedAny = lastCompletedTask != null && !lastCompletedTask.isBlank();
        boolean hasActiveGoal = activeGoalTitle != null && !activeGoalTitle.isBlank();

        if (hasCompletedAny) {
            sb.append("Foydalanuvchi so'nggi marta \"").append(lastCompletedTask).append("\" vazifasini bajargan.\n");
            sb.append("Jami bajargan vazifalari soni: ").append(completedTaskCount).append(".\n");
        } else {
            sb.append("Foydalanuvchi hali birorta ham vazifani bajarmagan — bu uning birinchi qadamlari, "
                    + "shunga mos, boshlash uchun ilhomlantiruvchi ohangda yoz.\n");
        }

        if (hasActiveGoal) {
            sb.append("Uning hozirgi faol maqsadi: \"").append(activeGoalTitle).append("\".\n");
        } else {
            sb.append("Hozircha faol maqsadi yo'q — /newgoal orqali maqsad qo'yishga undash mumkin.\n");
        }

        sb.append("\nQAT'IY QOIDALAR:\n");
        sb.append("1. Javobing FAQAT motivatsion xabarning o'zi bo'lsin — hech qanday izoh, sarlavha yoki "
                + "qo'shtirnoq bo'lmasin.\n");
        sb.append("2. Javob 2-3 gapdan oshmasin.\n");
        sb.append("3. Markdown yoki HTML teglaridan foydalanma, oddiy matn yoz.\n");
        sb.append(languageInstruction(language));
        return sb.toString();
    }

    @Override
    public String buildSosPrompt(ToneType tone, String currentTaskTitle, int estimatedMinutes, Language language) {
        return """
                Sen NextQadam ismli shaxsiy rivojlanish yordamchisisan. %s

                Foydalanuvchi "%s" nomli vazifani bajarishga qiynalyapti (taxminan %d daqiqa vaqt talab qiladi \
                deb baholangan, lekin hozir buni boshlashga kuchi yetmayapti).

                Vazifangiz: shu vazifani ANIQ 5 daqiqada bajarish mumkin bo'lgan, juda kichik va konkret bitta \
                harakatga qisqartir — shunday kichik bo'lsinki, foydalanuvchi bahona topa olmasin.

                QAT'IY QOIDALAR:
                1. Javobing FAQAT bitta qisqa jumla bo'lsin (masalan: "Faqat loyihaning bir faylini ochib, \
                sarlavhasini yoz").
                2. Hech qanday izoh, sarlavha, qo'shtirnoq yoki ro'yxat bo'lmasin — faqat harakatning o'zi.
                3. Markdown yoki HTML teglaridan foydalanma, oddiy matn yoz.
                """.formatted(FREE_CHAT_PERSONALITY.get(tone), currentTaskTitle, estimatedMinutes)
                + languageInstruction(language);
    }

    @Override
    public String buildEveningCheckinPrompt(String rawText, List<TaskSummaryForPrompt> todaysTasks, Language language) {
        String taskList = todaysTasks.stream()
                .map(task -> "- " + task.id() + " — " + task.title())
                .collect(Collectors.joining("\n"));
        return EVENING_CHECKIN_TEMPLATE.formatted(taskList, rawText) + languageInstruction(language);
    }

    @Override
    public String buildWeeklyRetrospectivePrompt(int totalTasks, int doneTasks, List<String> completedTaskTitles,
                                                  List<String> missedTaskTitles, Language language) {
        String completed = completedTaskTitles.isEmpty() ? "(yo'q)" : String.join(", ", completedTaskTitles);
        String missed = missedTaskTitles.isEmpty() ? "(yo'q)" : String.join(", ", missedTaskTitles);
        return WEEKLY_RETRO_TEMPLATE.formatted(totalTasks, doneTasks, completed, missed) + languageInstruction(language);
    }

    @Override
    public String buildGoalDriftPrompt(String goalTitle, List<String> recentTaskTitles, Language language) {
        String tasksText = recentTaskTitles.isEmpty()
                ? "(hech qanday vazifa topilmadi)"
                : String.join(", ", recentTaskTitles);
        String emptyNote = recentTaskTitles.isEmpty()
                ? "Eslatma: oxirgi 14 kunda hech qanday vazifa qayd etilmagan — bu holatda ball past chiqishi "
                        + "tabiiy, lekin izohda buni \"hali ma'lumot yetarli emas\" tarzida ifodala, "
                        + "foydalanuvchini ayblama."
                : "";
        return GOAL_DRIFT_TEMPLATE.formatted(goalTitle, tasksText, emptyNote) + languageInstruction(language);
    }
}
