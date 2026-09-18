package uz.nextqadam.bot.ai.impl;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.JsonEOFException;
import com.fasterxml.jackson.databind.ObjectMapper;

import uz.nextqadam.bot.ai.AiResponseParseException;
import uz.nextqadam.bot.ai.AiResponseParser;
import uz.nextqadam.bot.ai.dto.BrainDumpResult;
import uz.nextqadam.bot.ai.dto.GoalDecompositionResult;

@Component
public class AiResponseParserImpl implements AiResponseParser {

    private static final Logger log = LoggerFactory.getLogger(AiResponseParserImpl.class);

    // Spring Boot 4'ning Jackson auto-configuratsiyasi Jackson 3 (tools.jackson) uchun ObjectMapper bean
    // yaratadi, klassik com.fasterxml.jackson.databind.ObjectMapper uchun bean mavjud emas — shu sababli
    // o'zimiz instansiya yaratamiz.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public GoalDecompositionResult parseGoalDecomposition(String rawJson) {
        return parse(rawJson, GoalDecompositionResult.class);
    }

    @Override
    public BrainDumpResult parseBrainDump(String rawJson) {
        return parse(rawJson, BrainDumpResult.class);
    }

    private <T> T parse(String rawJson, Class<T> type) {
        String cleaned = stripMarkdownFence(rawJson);
        try {
            return objectMapper.readValue(cleaned, type);
        } catch (JsonProcessingException e) {
            String logId = UUID.randomUUID().toString();
            if (isLikelyTruncated(e)) {
                log.error("[{}] TRUNCATED: Gemini javobi kesilgan bo'lishi mumkin (token limit). rawJson={}",
                        logId, rawJson, e);
            } else {
                log.error("[{}] AI javobini JSON sifatida parse qilib bo'lmadi. rawJson={}", logId, rawJson, e);
            }
            throw new AiResponseParseException("AI javobini JSON sifatida o'qib bo'lmadi. logId=" + logId, e);
        }
    }

    private String stripMarkdownFence(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }

    // JsonEOFException — parser oqim tugashini kutmasdan matn tugaganda tashlanadi, bu odatda
    // generationConfig.maxOutputTokens chegarasiga yetib javob kesilganidan darak beradi.
    private boolean isLikelyTruncated(JsonProcessingException e) {
        return e instanceof JsonEOFException
                || (e.getMessage() != null && e.getMessage().contains("Unexpected end-of-input"));
    }
}
