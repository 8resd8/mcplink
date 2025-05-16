package kr.co.mcplink.domain.gemini.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.mcplink.domain.gemini.client.GeminiApiClient;
import kr.co.mcplink.domain.gemini.dto.GeminiRequestDto;
import kr.co.mcplink.domain.gemini.dto.GeminiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

@Service
@Slf4j
@RequiredArgsConstructor
public class FetchTagService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final GeminiApiClient geminiClient;

    public List<String> fetchTags(String serverName) {
        try {
            if (serverName == null || serverName.trim().isEmpty()) {
                log.warn("Server name is empty, cannot generate tags");
                return List.of(serverName);
            }

            GeminiRequestDto request = createTagRequest(serverName);
            GeminiResponseDto response = geminiClient.generateContent(request).block();
            List<String> tags = extractTags(response);

            if (tags.isEmpty()) {
                log.info("Failed to generate tags, using server name as tag: {}", serverName);
                return List.of(serverName);
            }

            return extractTags(response);
        } catch (Exception e) {
            log.error("Error fetching tags for server name '{}': {}", serverName, e.getMessage());
            return List.of(serverName);
        }
    }

    private GeminiRequestDto createTagRequest(String serverName) {
        String prompt = buildTagPrompt(serverName);
        return GeminiRequestDto.createRequest(prompt);
    }

    private String buildTagPrompt(String serverName) {
        return """
        Generate relevant tags from the following MCP server name: "%s"
        
        Tag Generation Rules:
        1. Exclude the words "mcp" and "server" from tags
        2. Convert all tags to lowercase
        3. For hyphenated words (e.g., "google-maps"):
            - Include the combined version with space instead of hyphen ("google maps")
            - Include each component as a separate tag ("google", "maps")
        4. Handle camelCase by converting to lowercase with spaces and breaking into parts:
            - Example: "googleMaps" → include "google maps", "google", "maps"
        5. For meaningful compound words, include both the original word and the expanded version
            - Example: "gdrive" → include both "gdrive" and "google drive"
            - Example: "mcpGdrive" → include "gdrive" and "google drive" (exclude "mcp")
        6. Keep acronyms as is (e.g., "aws", "gcp")
        7. Do not generate any single-character tags (like "a", "b", "c", "1", "2", "3")
        8. For compound phrases, keep them intact including any single characters:
            - Example: "what-a-wonderful" → include "what a wonderful" as a tag
            - Example: "gpt-mcp-1" → include "gpt mcp 1" as a tag
        
        Please respond ONLY with valid JSON in the following format:
        {
            "tags": ["tag1", "tag2", "tag3", ...]
        }
        """.formatted(serverName);
    }

    private List<String> extractTags(GeminiResponseDto response) {
        try {
            String generatedText = extractTextSafely(response);
            return parseJsonForTags(generatedText);
        } catch (Exception e) {
            log.error("Error parsing response to extract tags: ", e);
            return Collections.emptyList();
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

    private List<String> parseJsonForTags(String jsonText) {
        try {
            Pattern pattern = Pattern.compile("\\{[\\s\\S]*\\}", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(jsonText);

            if (matcher.find()) {
                String cleanedJson = matcher.group();
                JsonNode root = objectMapper.readTree(cleanedJson);

                if (root.has("tags") && root.get("tags").isArray()) {
                    return StreamSupport.stream(root.get("tags").spliterator(), false)
                            .map(JsonNode::asText)
                            .toList();
                }
            }

            log.warn("Failed to extract tags JSON from response: {}", jsonText);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to parse JSON response for tags: {}", jsonText, e);
            return Collections.emptyList();
        }
    }
}
