package uz.nextqadam.bot.ai.dto;

import java.util.List;

public record EveningCheckinResult(List<TaskClassification> classifications, String overallMood) {
}
