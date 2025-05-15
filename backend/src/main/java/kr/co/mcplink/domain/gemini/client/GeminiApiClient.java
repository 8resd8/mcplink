package kr.co.mcplink.domain.gemini.client;

import kr.co.mcplink.domain.gemini.dto.GeminiRequestDto;
import kr.co.mcplink.domain.gemini.dto.GeminiResponseDto;
import kr.co.mcplink.global.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class GeminiApiClient {

    @Value("${spring.gemini.api-key}")
    private String geminiApiKey;

    private final WebClient geminiClient;
    private final String model = "gemini-2.0-flash";

    public GeminiApiClient(@Qualifier("geminiClient") WebClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    public Mono<GeminiResponseDto> generateContent(GeminiRequestDto request) {
        return geminiClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(Constants.GEMINI_GENERATE_CONTENT_PATH)
                        .queryParam("key", geminiApiKey)
                        .build(model))
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GeminiResponseDto.class)
                .doOnError(e -> log.error("Error calling Gemini API: {}", e.getMessage()));
    }
}
