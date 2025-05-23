package kr.co.mcplink.domain.auth.dto;

import kr.co.mcplink.domain.user.dto.OAuthUserInfo;

public record OAuthSsafyUserInfo(SsafyUserInfoDto userInfo) implements OAuthUserInfo {
	@Override
	public String getProviderId() {
		return userInfo.ssafyUserId();
	}

	@Override
	public String getEmail() {
		return userInfo.email();
	}

	@Override
	public String getName() {
		return userInfo.name();
	}
}