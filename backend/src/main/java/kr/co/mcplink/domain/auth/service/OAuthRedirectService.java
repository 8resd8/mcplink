package kr.co.mcplink.domain.auth.service;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.mcplink.domain.auth.dto.LoginResponse;
import kr.co.mcplink.global.common.SocialProvider;

public interface OAuthRedirectService {

	/**
	 * OAuth2 공급자의 인증 페이지 URL을 반환합니다.
	 * @return 인증 페이지 URL
	 */
	String getRedirectUrl();

	SocialProvider getProvider();
}
