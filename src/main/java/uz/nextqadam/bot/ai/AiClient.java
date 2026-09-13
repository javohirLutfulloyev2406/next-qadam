package uz.nextqadam.bot.ai;

public interface AiClient {

    String complete(String systemPrompt, String userPrompt);
}