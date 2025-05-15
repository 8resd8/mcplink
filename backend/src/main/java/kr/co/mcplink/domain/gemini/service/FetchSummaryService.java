package kr.co.mcplink.domain.gemini.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.mcplink.domain.gemini.dto.GeminiRequestDto;
import kr.co.mcplink.domain.gemini.dto.GeminiResponseDto;
import kr.co.mcplink.domain.gemini.dto.ReadmeSummaryDto;
import kr.co.mcplink.domain.mcpserverv2.entity.McpServerV2;
import kr.co.mcplink.domain.mcpserverv2.repository.McpServerV2Repository;
import kr.co.mcplink.global.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class FetchSummaryService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final McpServerV2Repository mcpServerV2Repository;

    @Value("${spring.gemini.api-key}")
    private String geminiApiKey;

    private final WebClient geminiClient;
    private final String model = "gemini-2.0-flash";

    public FetchSummaryService(
            @Qualifier("geminiClient") WebClient geminiClient,
            McpServerV2Repository mcpServerV2Repository
    ) {
        this.geminiClient = geminiClient;
        this.mcpServerV2Repository = mcpServerV2Repository;
    }

    public ReadmeSummaryDto fetchSummary(String readmeContent) {
        return fetchSummary(readmeContent, null);
    }

    public ReadmeSummaryDto fetchSummary(String readmeContent, String serverId) {
        try {
            GeminiRequestDto request = createRequest(readmeContent);
            GeminiResponseDto response = generateContent(request).block();
            ReadmeSummaryDto result = toReadmeSummaryDto(response);

            if (serverId != null && (result.summary() == null || result.summary().isEmpty() ||
                    result.summary().startsWith("Failed to") || result.summary().startsWith("Error"))) {
                return generateFallbackSummary(serverId);
            }

            return result;
        } catch (Exception e) {
            log.error("Error fetching summary: ", e);

            if (serverId != null) {
                return generateFallbackSummary(serverId);
            }

            return new ReadmeSummaryDto("Failed to generate summary", Collections.emptyList());
        }
    }

    private ReadmeSummaryDto generateFallbackSummary(String serverId) {
        try {
            Optional<McpServerV2> serverOpt = mcpServerV2Repository.findById(serverId);

            if (serverOpt.isPresent()) {
                McpServerV2 server = serverOpt.get();
                String serverName = server.getDetail().getName();
                String serverUrl = server.getUrl();

                String fallbackSummary = String.format(
                        Constants.GEMINI_FALLBACK_SUMMARY_TEXT,
                        serverName,
                        serverUrl
                );

                return new ReadmeSummaryDto(fallbackSummary, Collections.emptyList());
            } else {
                log.warn("Server not found for id: {}", serverId);
                return new ReadmeSummaryDto(
                        Constants.GEMINI_FALLBACK_SUMMARY_TEXT,
                        Collections.emptyList()
                );
            }
        } catch (Exception e) {
            log.error("Error generating fallback summary for serverId {}: {}", serverId, e.getMessage());
            return new ReadmeSummaryDto(
                    Constants.GEMINI_DEFAULT_FALLBACK_TEXT,
                    Collections.emptyList()
            );
        }
    }

    private Mono<GeminiResponseDto> generateContent(GeminiRequestDto request) {
        return geminiClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(Constants.GEMINI_GENERATE_CONTENT_PATH)
                        .queryParam("key", geminiApiKey)
                        .build(model))
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GeminiResponseDto.class)
                .doOnError(e -> log.error("Error generating content: {}", e.getMessage()));
    }

    private GeminiRequestDto createRequest(String readmeContent) {
        String prompt = buildPrompt(readmeContent);
        return GeminiRequestDto.createRequest(prompt);
    }

    private String buildPrompt(String readmeContent) {
        return """
        Analyze the following README content and extract a summary and related tags.
        
        For the summary:
        - Write 1-2 concise, clear sentences that explain what this MCP server implementation does
        - Focus on the core capabilities and integrations of this server
        - Use present tense, active voice
        - Be specific about technologies and protocols used
        - Write in professional English
        
        Please respond ONLY with valid JSON in the following format:
        {
          "summary": "Your concise functional description here",
          "tags": ["tag1", "tag2", "tag3", ...]
        }
        
        README content:
        %s
        """.formatted(readmeContent);
    }

    private ReadmeSummaryDto toReadmeSummaryDto(GeminiResponseDto response) {
        try {
            String generatedText = extractTextSafely(response);
            return parseJsonResponse(generatedText);
        } catch (Exception e) {
            log.error("Error parsing response to ReadmeSummaryDto: ", e);
            return new ReadmeSummaryDto("Error parsing response", Collections.emptyList());
        }
    }

    private String extractTextSafely(GeminiResponseDto response) {
        return Optional.ofNullable(response)
                .map(GeminiResponseDto::candidates)
                .filter(candidates -> !candidates.isEmpty())
                .map(candidates -> candidates.get(0))
                .map(GeminiResponseDto.CandidateDto::content)
                .map(GeminiResponseDto.ContentDto::parts)
                .filter(parts -> !parts.isEmpty())
                .map(parts -> parts.get(0))
                .map(GeminiResponseDto.PartDto::text)
                .orElse("");
    }

    private ReadmeSummaryDto parseJsonResponse(String jsonText) {
        try {
            Pattern pattern = Pattern.compile("\\{[\\s\\S]*\\}", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(jsonText);

            if (matcher.find()) {
                String cleanedJson = matcher.group();
                JsonNode root = objectMapper.readTree(cleanedJson);

                String summary = root.has("summary") ? root.get("summary").asText() : "";

                List<String> tags = new ArrayList<>();
                if (root.has("tags") && root.get("tags").isArray()) {
                    tags = StreamSupport.stream(root.get("tags").spliterator(), false)
                            .map(JsonNode::asText)
                            .toList();
                }

                return new ReadmeSummaryDto(summary, tags);
            }

            log.warn("Failed to extract JSON from response: {}", jsonText);
            return new ReadmeSummaryDto(
                    jsonText.length() > 100 ? jsonText.substring(0, 100) + "..." : jsonText,
                    Collections.emptyList()
            );
        } catch (Exception e) {
            log.error("Failed to parse JSON response: {}", jsonText, e);
            return new ReadmeSummaryDto("Failed to parse response", Collections.emptyList());
        }
    }
}