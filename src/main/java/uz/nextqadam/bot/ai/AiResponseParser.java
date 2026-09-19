package uz.nextqadam.bot.ai;

import uz.nextqadam.bot.ai.dto.BrainDumpResult;
import uz.nextqadam.bot.ai.dto.EveningCheckinResult;
import uz.nextqadam.bot.ai.dto.GoalDecompositionResult;
import uz.nextqadam.bot.ai.dto.GoalDriftResult;
import uz.nextqadam.bot.ai.dto.WeeklyRetrospective;

public interface AiResponseParser {

    GoalDecompositionResult parseGoalDecomposition(String rawJson);

    BrainDumpResult parseBrainDump(String rawJson);

    EveningCheckinResult parseEveningCheckin(String rawJson);

    WeeklyRetrospective parseWeeklyRetrospective(String rawJson);

    GoalDriftResult parseGoalDrift(String rawJson);
}