package kr.co.mcplink.domain.user.dto;

public interface OAuthUserInfo {
	String getProviderId();
	String getEmail();
	String getName();
}