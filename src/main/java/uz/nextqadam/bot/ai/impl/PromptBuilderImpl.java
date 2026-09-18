package uz.nextqadam.bot.ai.impl;

import org.springframework.stereotype.Component;

import uz.nextqadam.bot.ai.PromptBuilder;

@Component
public class PromptBuilderImpl implements PromptBuilder {

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

    @Override
    public String buildGoalDecompositionPrompt(String goalDescription, String memoryContext) {
        String prompt = TEMPLATE.formatted(goalDescription);
        if (memoryContext != null && !memoryContext.isBlank()) {
            prompt += "\n\nFoydalanuvchi haqida ma'lum ma'lumotlar:\n" + memoryContext;
        }
        return prompt;
    }

    @Override
    public String buildBrainDumpPrompt(String rawText) {
        return BRAIN_DUMP_TEMPLATE.formatted(rawText);
    }
}
