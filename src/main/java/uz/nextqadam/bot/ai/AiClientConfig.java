package uz.nextqadam.bot.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "ai.client")
public class AiClientConfig {

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    private String apiKey;
    private String model = "gemini-3.6-flash";
    private int timeoutSeconds = 20;

    @Bean
    public WebClient geminiWebClient(WebClient.Builder builder) {
        return builder.baseUrl(GEMINI_BASE_URL).build();
    }
}