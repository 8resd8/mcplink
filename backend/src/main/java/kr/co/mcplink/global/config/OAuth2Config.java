package kr.co.mcplink.global.config;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import kr.co.mcplink.domain.auth.service.OAuthRedirectService;
import kr.co.mcplink.global.common.SocialProvider;

@Configuration
public class OAuth2Config {

	@Bean
	public Map<SocialProvider, OAuthRedirectService> oauth2RedirectServices(List<OAuthRedirectService> services) {
		Map<SocialProvider, OAuthRedirectService> redirectServices = new EnumMap<>(SocialProvider.class);
		services.forEach(service -> redirectServices.put(service.getProvider(), service));
		return redirectServices;
	}
}