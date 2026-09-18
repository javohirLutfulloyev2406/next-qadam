package uz.nextqadam.bot.ai.dto;

import java.util.List;

public record BrainDumpResult(List<String> tasks, List<String> ideas, List<ReminderDraft> reminders) {
}
