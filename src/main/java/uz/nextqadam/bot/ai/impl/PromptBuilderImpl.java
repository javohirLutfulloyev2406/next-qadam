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

    @Override
    public String buildGoalDecompositionPrompt(String goalDescription) {
        return TEMPLATE.formatted(goalDescription);
    }
}
