package kr.co.mcplink.domain.gemini.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GeminiRequestDto(
        List<ContentRequest> contents
) {
    public static GeminiRequestDto createRequest(String prompt) {
        PartRequest part = new PartRequest(prompt);
        ContentRequest content = new ContentRequest(List.of(part));
        return new GeminiRequestDto(List.of(content));
    }

    public record ContentRequest(
            List<PartRequest> parts
    ) {

    }

    public record PartRequest(
            @JsonProperty("text") String text
    ) {

    }
}