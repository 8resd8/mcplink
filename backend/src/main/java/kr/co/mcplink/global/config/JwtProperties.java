package kr.co.mcplink.global.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "jwt")
@Validated
public record JwtProperties(
	@NotNull
	Long accessExpiration,

	@NotEmpty
	String secret,

	@NotEmpty
	List<String> securePath,

	@NotEmpty
	List<String> oauthSsafyPath
) {
}
