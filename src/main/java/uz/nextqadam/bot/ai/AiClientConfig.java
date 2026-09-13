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

    private static final String ANTHROPIC_BASE_URL = "https://api.anthropic.com/v1";

    private String apiKey;
    private String model = "claude-sonnet-4-6";
    private int timeoutSeconds = 20;

    @Bean
    public WebClient anthropicWebClient(WebClient.Builder builder) {
        return builder.baseUrl(ANTHROPIC_BASE_URL).build();
    }
}