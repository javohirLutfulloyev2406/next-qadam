package uz.nextqadam.bot.plan;

import java.util.UUID;

public interface PlanService {

    void generateWeeklyPlan(UUID userId);

    void regeneratePlan(UUID goalId);
}
