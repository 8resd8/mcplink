package kr.co.mcplink.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SsafyUserInfoDto(
	@JsonProperty("userId") String ssafyUserId, // 고유 식별 ID
	String email,
	String name
) {
}
