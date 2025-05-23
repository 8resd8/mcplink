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

	/**
	 * 로그인 성공 후 리디렉션을 처리합니다.
	 * @param request HttpServletRequest
	 * @param response HttpServletResponse
	 * @param loginResponse 로그인 응답 정보 (토큰 등 포함)
	 * @param targetUrlParam 프론트엔드에서 전달받은 리디렉션 대상 URL (선택 사항)
	 * @throws IOException
	 */
	void handleLoginSuccess(HttpServletRequest request, HttpServletResponse response, LoginResponse loginResponse,
		String targetUrlParam) throws IOException;

	/**
	 * 로그인 실패 후 리디렉션을 처리합니다.
	 * @param request HttpServletRequest
	 * @param response HttpServletResponse
	 * @param exception 발생한 예외
	 * @param targetUrlParam 프론트엔드에서 전달받은 리디렉션 대상 URL (선택 사항)
	 * @throws IOException
	 */
	void handleLoginFailure(HttpServletRequest request, HttpServletResponse response, Exception exception,
		String targetUrlParam) throws IOException;

	/**
	 * 이 서비스가 지원하는 SocialProvider를 반환합니다.
	 * @return SocialProvider
	 */
	SocialProvider getProvider();

}
