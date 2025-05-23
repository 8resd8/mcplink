package kr.co.mcplink.global.common;

import lombok.Getter;

@Getter
public enum SocialProvider {
	SSAFY("ssafy"),
	GITHUB("github"),
	GOOGLE("google"),
	KAKAO("kakao"),
	NAVER("naver");

	private final String providerName;

	SocialProvider(String providerName) {
		this.providerName = providerName;
	}

}