package uz.nextqadam.bot.ai.impl;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.Exceptions;
import reactor.util.retry.Retry;

import uz.nextqadam.bot.ai.AiClient;
import uz.nextqadam.bot.ai.AiClientConfig;
import uz.nextqadam.bot.ai.AiClientException;

@Component
public class AiClientImpl implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(AiClientImpl.class);
    private static final int MAX_OUTPUT_TOKENS = 4096;
    private static final double TEMPERATURE = 0.7;
    // gemini-3.x flash modellari "thinking" (fikrlash) rejimida ishlaydi — ko'rinmas fikrlash tokenlari
    // ham maxOutputTokens byudjetidan hisoblanadi, shu sababli oddiy so'rovlarda ko'rinadigan matn
    // chiqmasdan javob kesilib qolishi mumkin edi. Bizning barcha holatlarimiz (JSON ajratish, qisqa
    // suhbat javobi) uzoq fikrlashni talab qilmaydi, shuning uchun thinkingBudget=0 bilan o'chiramiz.
    private static final int THINKING_BUDGET = 0;
    private static final Set<String> NON_BLOCKING_FINISH_REASONS = Set.of("STOP", "MAX_TOKENS", "");

    // Vaqtinchalik server xatolari — qayta urinishga arziydi, chunki keyingi urinishda tuzalishi mumkin.
    private static final Set<Integer> RETRYABLE_HTTP_STATUS_CODES = Set.of(
            HttpStatus.SERVICE_UNAVAILABLE.value(), HttpStatus.BAD_GATEWAY.value(),
            HttpStatus.TOO_MANY_REQUESTS.value());
    private static final int RETRY_MAX_ATTEMPTS = 3;
    private static final Duration RETRY_MIN_BACKOFF = Duration.ofSeconds(1);
    private static final Duration RETRY_MAX_BACKOFF = Duration.ofSeconds(8);

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
        return callGemini(systemPrompt, userPrompt, true);
    }

    @Override
    public String completeText(String systemPrompt, String userPrompt) {
        return callGemini(systemPrompt, userPrompt, false);
    }

    private String callGemini(String systemPrompt, String userPrompt, boolean jsonMode) {
        String logId = UUID.randomUUID().toString();

        Map<String, Object> generationConfig = jsonMode
                ? Map.of(
                        "maxOutputTokens", MAX_OUTPUT_TOKENS,
                        "temperature", TEMPERATURE,
                        "response_mime_type", "application/json",
                        "thinkingConfig", Map.of("thinkingBudget", THINKING_BUDGET))
                : Map.of(
                        "maxOutputTokens", MAX_OUTPUT_TOKENS,
                        "temperature", TEMPERATURE,
                        "thinkingConfig", Map.of("thinkingBudget", THINKING_BUDGET));

        Map<String, Object> requestBody = Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userPrompt)))),
                "generationConfig", generationConfig
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
                    .retryWhen(Retry.backoff(RETRY_MAX_ATTEMPTS, RETRY_MIN_BACKOFF)
                            .maxBackoff(RETRY_MAX_BACKOFF)
                            .filter(this::isRetryable)
                            .doBeforeRetry(retrySignal -> log.warn(
                                    "[{}] Gemini so'rovi muvaffaqiyatsiz, qayta urinilmoqda ({}-urinish)",
                                    logId, retrySignal.totalRetries() + 1)))
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
            boolean transientFailure = isRetryable(unwrapRetryExhausted(e));
            throw new AiClientException("AI chaqiruvida xatolik yuz berdi. logId=" + logId, e, transientFailure);
        }
    }

    /**
     * Retry'lar tugagach, Reactor asl xatoni "retry exhausted" wrapper'iga o'raydi — shu sababli
     * transientFailure'ni to'g'ri aniqlash uchun avval asl (oxirgi urinishdagi) xatoni ajratib olamiz.
     */
    private Throwable unwrapRetryExhausted(Throwable throwable) {
        if (Exceptions.isRetryExhausted(throwable) && throwable.getCause() != null) {
            return throwable.getCause();
        }
        return throwable;
    }

    /**
     * Qayta urinishga arziydigan (vaqtinchalik) xatolar: 503/502/429 yoki so'rov timeout bo'lishi.
     * Boshqa hollarda (400/401/403/404 va h.k.) xato o'zgarmaydi — darhol yuqoriga uzatiladi.
     */
    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof WebClientResponseException webClientResponseException) {
            return RETRYABLE_HTTP_STATUS_CODES.contains(webClientResponseException.getStatusCode().value());
        }
        return throwable instanceof TimeoutException;
    }
}
