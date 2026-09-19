package uz.nextqadam.bot.ai.dto;

import java.util.UUID;

public record TaskClassification(UUID taskId, boolean done, String reason) {
}
