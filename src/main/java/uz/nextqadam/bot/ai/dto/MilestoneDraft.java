package uz.nextqadam.bot.ai.dto;

import java.util.List;

public record MilestoneDraft(String title, String period, List<TaskDraft> tasks) {
}