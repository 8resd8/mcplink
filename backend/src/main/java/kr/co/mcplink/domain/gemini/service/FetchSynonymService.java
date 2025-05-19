package kr.co.mcplink.domain.gemini.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.mcplink.domain.gemini.client.GeminiApiClient;
import kr.co.mcplink.domain.gemini.dto.GeminiRequestDto;
import kr.co.mcplink.domain.gemini.dto.GeminiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

@Service
@Slf4j
@RequiredArgsConstructor
public class FetchSynonymService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final GeminiApiClient geminiClient;

    public List<String> fetchSynonyms(String tag) {

        try {
            if (tag == null || tag.trim().isEmpty()) {
                log.warn("Tag is empty, cannot generate synonyms");
                return Collections.emptyList();
            }

            GeminiRequestDto request = createSynonymRequest(tag);
            GeminiResponseDto response = geminiClient.generateContent(request).block();
            List<String> koreanSynonyms = extractSynonyms(response);

            if (koreanSynonyms.isEmpty()) {
                log.info("No Korean synonyms generated for tag: {}", tag);
                return List.of(tag);
            }

            List<String> result = new ArrayList<>();
            result.add(tag);
            result.addAll(koreanSynonyms);

            return result;
        } catch (Exception e) {
            log.error("Error fetching synonyms for tag '{}': {}", tag, e.getMessage());
            return List.of(tag);
        }
    }

    private GeminiRequestDto createSynonymRequest(String tag) {
        String prompt = buildSynonymPrompt(tag);
        return GeminiRequestDto.createRequest(prompt);
    }

    private String buildSynonymPrompt(String englishTerm) {
        return """
        Generate Korean synonyms for the following English technical term: "%s"
        
        Rules:
        1. Provide Korean terms that have the same meaning as the English term
        2. Include transliterations (how Koreans would pronounce the English term)
        3. Include common Korean usage or translations
        4. Provide 1-3 Korean synonyms for the term
        5. Do NOT include ANY English words in your response, especially "%s" itself
        6. Do NOT include the original English term in your response
        
        Examples of good responses:
        - For "chat": ["챗", "채팅"]
        - For "google": ["구글"]
        - For "aws": ["아마존 웹 서비스", "에이더블유에스"]
        - For "maps": ["맵스", "지도"]
        - For "google maps": ["구글 맵스", "구글 지도"]
        
        Examples of BAD responses (do NOT do this):
        - For "aws": ["AWS", "아마존 웹 서비스", "에이더블유에스"] ← WRONG, contains English "AWS"
        - For "api": ["API", "에이피아이"] ← WRONG, contains English "API"
        
        Please respond ONLY with valid JSON in the following format:
        {
            "synonyms": ["korean_synonym1", "korean_synonym2", "korean_synonym3"]
        }
        """.formatted(englishTerm, englishTerm);
    }

    private List<String> extractSynonyms(GeminiResponseDto response) {
        try {
            String generatedText = extractTextSafely(response);
            return parseJsonForSynonyms(generatedText);
        } catch (Exception e) {
            log.error("Error parsing response to extract synonyms: ", e);
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

    private List<String> parseJsonForSynonyms(String jsonText) {
        try {
            Pattern pattern = Pattern.compile("\\{[\\s\\S]*\\}", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(jsonText);

            if (matcher.find()) {
                String cleanedJson = matcher.group();
                JsonNode root = objectMapper.readTree(cleanedJson);

                if (root.has("synonyms") && root.get("synonyms").isArray()) {
                    return StreamSupport.stream(root.get("synonyms").spliterator(), false)
                            .map(JsonNode::asText)
                            .filter(text -> !text.isEmpty())
                            .toList();
                }
            }

            log.warn("Failed to extract synonyms JSON from response: {}", jsonText);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to parse JSON response for synonyms: {}", jsonText, e);
            return Collections.emptyList();
        }
    }
}