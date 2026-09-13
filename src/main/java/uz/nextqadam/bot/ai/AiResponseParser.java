package uz.nextqadam.bot.ai;

import uz.nextqadam.bot.ai.dto.GoalDecompositionResult;

public interface AiResponseParser {

    GoalDecompositionResult parseGoalDecomposition(String rawJson);
}