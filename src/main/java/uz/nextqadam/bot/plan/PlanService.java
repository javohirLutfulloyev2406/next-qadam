package uz.nextqadam.bot.plan;

import java.util.List;
import java.util.UUID;

import uz.nextqadam.bot.goal.Task;

public interface PlanService {

    /**
     * Erkin matnni AI orqali Task/Idea/Reminder toifalariga ajratadi va har birini tegishli
     * entity sifatida saqlaydi.
     */
    BrainDumpSummary processBrainDump(UUID userId, String rawText);

    List<Task> getTodayPriorityTasks(UUID userId);

    /**
     * Foydalanuvchining barcha tasklarida is_today_priority'ni tozalaydi, so'ng berilgan ro'yxatdagi
     * (max 3 ta) tasklarni ustuvor deb belgilaydi va bu tanlovni CheckIn (MORNING) sifatida yozadi.
     */
    void setTodayPriorities(UUID userId, List<UUID> taskIds);

    record BrainDumpSummary(int taskCount, int ideaCount, int reminderCount) {
    }
}
