package uz.nextqadam.bot.ai.impl;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import uz.nextqadam.bot.ai.AiClient;
import uz.nextqadam.bot.ai.AiClientConfig;
import uz.nextqadam.bot.ai.AiClientException;

@Component
public class AiClientImpl implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(AiClientImpl.class);
    private static final int MAX_OUTPUT_TOKENS = 2000;
    private static final double TEMPERATURE = 0.7;
    private static final Set<String> NON_BLOCKING_FINISH_REASONS = Set.of("STOP", "MAX_TOKENS", "");

    // Spring Boot 4'ning WebClient auto-configuratsiyasi Jackson 3 (tools.jackson) asosida ishlaydi,
    // shu sababli klassik com.fasterxml.jackson.databind.JsonNode uchun HttpMessageReader mavjud emas —
    // javobni String sifatida olib, o'zimizning ObjectMapper bilan qo'lda parse qilamiz.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WebClient geminiWebClient;
    private final AiClientConfig config;

    public AiClientImpl(WebClient geminiWebClient, AiClientConfig config) {
        this.geminiWebClient = geminiWebClient;
        this.config = config;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        String logId = UUID.randomUUID().toString();
        Map<String, Object> requestBody = Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userPrompt)))),
                "generationConfig", Map.of(
                        "maxOutputTokens", MAX_OUTPUT_TOKENS,
                        "temperature", TEMPERATURE,
                        "response_mime_type", "application/json"
                )
        );

        try {
            String rawResponse = geminiWebClient.post()
                    .uri(uriBuilder -> {
                        URI uri = uriBuilder.path("/models/{model}:generateContent").build(config.getModel());
                        log.info("[{}] Gemini so'rov URL: {}", logId, uri);
                        return uri;
                    })
                    .header("x-goog-api-key", config.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                    .retry(1)
                    .block();

            if (rawResponse == null) {
                throw new AiClientException("AI provayderdan bo'sh javob keldi. logId=" + logId, null);
            }

            JsonNode response = OBJECT_MAPPER.readTree(rawResponse);
            JsonNode candidates = response.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                throw new AiClientException("Gemini javob qaytarmadi (candidates bo'sh). logId=" + logId, null);
            }

            JsonNode firstCandidate = candidates.get(0);
            String finishReason = firstCandidate.path("finishReason").asText("");
            if (!NON_BLOCKING_FINISH_REASONS.contains(finishReason)) {
                throw new AiClientException(
                        "Gemini javobni bloklanishi (finishReason=" + finishReason + "). logId=" + logId, null);
            }

            return firstCandidate.path("content").path("parts").path(0).path("text").asText();
        } catch (AiClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("[{}] AI chaqiruvida xatolik yuz berdi", logId, e);
            throw new AiClientException("AI chaqiruvida xatolik yuz berdi. logId=" + logId, e);
        }
    }
}
