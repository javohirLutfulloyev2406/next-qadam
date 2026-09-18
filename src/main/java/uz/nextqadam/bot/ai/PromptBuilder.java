package uz.nextqadam.bot.ai;

public interface PromptBuilder {

    String buildGoalDecompositionPrompt(String goalDescription, String memoryContext);
}