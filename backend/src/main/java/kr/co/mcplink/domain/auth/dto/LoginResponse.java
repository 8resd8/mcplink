package kr.co.mcplink.domain.auth.dto;

public record LoginResponse(
	String accessToken,
	Long accessExpiredAt
) {
}
