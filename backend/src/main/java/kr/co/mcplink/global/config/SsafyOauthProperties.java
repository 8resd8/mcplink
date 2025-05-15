package kr.co.mcplink.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;

@ConfigurationProperties(prefix = "ssafy.oauth")
@Validated
public record SsafyOauthProperties(
	@NotEmpty
	String clientId,

	@NotEmpty
	String clientSecret,

	@NotEmpty
	String redirectUri,

	@NotEmpty
	String tokenUri,

	@NotEmpty
	String userInfoUri,

	@NotEmpty
	String authorizationUri,

	@NotEmpty
	String scope
) {
}
