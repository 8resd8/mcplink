package kr.co.mcplink.domain.github.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubSearchResultDto(
        String owner,
        String repo
) {
    @JsonCreator
    public GithubSearchResultDto(
            @JsonProperty("owner") JsonNode ownerNode,
            @JsonProperty("name")  String repo
    ) {
        this(ownerNode.get("login").asText(), repo);
    }
}