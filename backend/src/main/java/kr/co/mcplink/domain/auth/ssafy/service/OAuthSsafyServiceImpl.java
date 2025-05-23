package kr.co.mcplink.domain.auth.ssafy.service;

import java.io.IOException;

import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.mcplink.domain.auth.ssafy.dto.LoginResponse;
import kr.co.mcplink.global.common.SocialProvider;
import kr.co.mcplink.global.config.SsafyOauthProperties;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OAuthSsafyServiceImpl implements OAuthRedirectService {

	private final SsafyOauthProperties ssafyProperties;

	@Override
	public String getRedirectUrl() {
		return UriComponentsBuilder.fromUriString(ssafyProperties.authorizationUri())
			.queryParam("response_type", "code")
			.queryParam("client_id", ssafyProperties.clientId())
			.queryParam("redirect_uri", ssafyProperties.redirectUri())
			.queryParam("scope", ssafyProperties.scope().replace(",", " ")).toUriString();
	}

	@Override
	public void handleLoginSuccess(HttpServletRequest request, HttpServletResponse response,
		LoginResponse loginResponse, String targetUrlParam) throws IOException {

	}

	@Override
	public void handleLoginFailure(HttpServletRequest request, HttpServletResponse response, Exception exception,
		String targetUrlParam) throws IOException {

	}

	@Override
	public SocialProvider getProvider() {
		return SocialProvider.SSAFY;
	}
}
