package kr.co.mcplink.domain.gemini.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiResponseDto(
        List<CandidateDto> candidates
) {
    @JsonCreator
    public GeminiResponseDto(
        @JsonProperty("candidates") List<CandidateDto> candidates
    ) {
        this.candidates = candidates != null ? candidates : Collections.emptyList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CandidateDto(
        ContentDto content
    ) {
        @JsonCreator
        public CandidateDto(
                @JsonProperty("content") ContentDto content
        ) {
            this.content = content;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContentDto(
        List<PartDto> parts
    ) {
        @JsonCreator
        public ContentDto(
                @JsonProperty("parts") List<PartDto> parts
        ) {
            this.parts = parts != null ? parts : Collections.emptyList();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PartDto(
        String text
    ) {
        @JsonCreator
        public PartDto(
                @JsonProperty("text") String text
        ) {
            this.text = text;
        }
    }
}