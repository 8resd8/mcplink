package kr.co.mcplink.domain.user.dto;

public record UserInfoDto(
	Long userId,
	String name,
	String email,
	String nickname
) {
}
