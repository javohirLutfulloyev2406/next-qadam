package uz.nextqadam.bot.ai.impl;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;

import uz.nextqadam.bot.ai.AiClient;
import uz.nextqadam.bot.ai.AiClientConfig;
import uz.nextqadam.bot.ai.AiClientException;

@Component
public class AiClientImpl implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(AiClientImpl.class);
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int MAX_TOKENS = 2000;

    private final WebClient anthropicWebClient;
    private final AiClientConfig config;

    public AiClientImpl(WebClient anthropicWebClient, AiClientConfig config) {
        this.anthropicWebClient = anthropicWebClient;
        this.config = config;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        String logId = UUID.randomUUID().toString();
        Map<String, Object> requestBody = Map.of(
                "model", config.getModel(),
                "max_tokens", MAX_TOKENS,
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", userPrompt))
        );

        try {
            JsonNode response = anthropicWebClient.post()
                    .uri("/messages")
                    .header("x-api-key", config.getApiKey())
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                    .retry(1)
                    .block();

            if (response == null) {
                throw new AiClientException("AI provayderdan bo'sh javob keldi. logId=" + logId, null);
            }
            return response.path("content").path(0).path("text").asText();
        } catch (AiClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("[{}] AI chaqiruvida xatolik yuz berdi", logId, e);
            throw new AiClientException("AI chaqiruvida xatolik yuz berdi. logId=" + logId, e);
        }
    }
}