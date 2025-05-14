package kr.co.mcplink.domain.auth.ssafy.controller;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletResponse;
import kr.co.mcplink.domain.auth.ssafy.dto.LoginResponse;
import kr.co.mcplink.domain.auth.ssafy.service.SsafyAuthService;
import kr.co.mcplink.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/v1/auth/ssafy")
@RequiredArgsConstructor
public class SsafyAuthController {

	private final SsafyAuthService ssafyAuthService;

	@Value("${ssafy.oauth.authorization-uri}")
	private String authorizationUri;

	@Value("${ssafy.oauth.client-id}")
	private String clientId;

	@Value("${ssafy.oauth.redirect-uri}")
	private String redirectUri;

	@Value("${ssafy.oauth.scope}")
	private String scope;

	/**
	 * SSAFY 로그인 요청을 시작합니다.
	 * 사용자를 SSAFY 인증 페이지로 리디렉션합니다.
	 * @param response HttpServletResponse for redirection
	 */
	@GetMapping("/login")
	public void redirectToSsafyLogin(HttpServletResponse response) throws IOException {
		// CSRF 추가

		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(authorizationUri)
			.queryParam("response_type", "code")
			.queryParam("client_id", clientId)
			.queryParam("redirect_uri", redirectUri)
			.queryParam("scope", scope.replace(",", " "));

		String redirectUrl = builder.toUriString();
		log.info("Redirecting to SSAFY login: {}", redirectUrl);
		response.sendRedirect(redirectUrl);
	}

	/**
	 * SSAFY로부터의 인증 콜백을 처리합니다.
	 * @param code Authorization Code
	 * @param state CSRF 방어용 state (사용했다면 검증 필요)
	 * @return ApiResponse containing JWT
	 */
	@GetMapping("/callback")
	public ApiResponse<LoginResponse> handleSsafyCallback(@RequestParam("code") String code,
		@RequestParam(name = "state", required = false) String state) {
		log.info("Received SSAFY callback with code: {}", code);
		String jwtToken = ssafyAuthService.processSsafyLogin(code);

		if (jwtToken != null) {
			log.info("SSAFY login successful, JWT issued.");
			return ApiResponse.success(HttpStatus.OK.toString(), "SSAFY 로그인 성공", new LoginResponse(jwtToken));
		} else {
			log.error("SSAFY login failed, JWT not issued.");
			return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.toString(), "SSAFY 로그인 처리 중 오류가 발생했습니다.");
		}
	}
}