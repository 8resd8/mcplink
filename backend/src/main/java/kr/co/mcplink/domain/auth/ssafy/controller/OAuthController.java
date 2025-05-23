package kr.co.mcplink.domain.auth.ssafy.controller;

import static kr.co.mcplink.global.common.Constants.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import kr.co.mcplink.domain.auth.ssafy.dto.LoginResponse;
import kr.co.mcplink.domain.auth.ssafy.service.OAuthRedirectService;
import kr.co.mcplink.domain.user.service.OAuthService;
import kr.co.mcplink.global.common.ApiResponse;
import kr.co.mcplink.global.common.SocialProvider;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class OAuthController {

	private final OAuthService oAuthService;
	private final Map<SocialProvider, OAuthRedirectService> redirectServices;

	@GetMapping("/{provider}/login")
	public ResponseEntity<ApiResponse<Void>> redirectToOAuthLogin(@PathVariable("provider") String provider, HttpServletResponse response)
		throws IOException {

		SocialProvider socialProvider = SocialProvider.valueOf(provider.toUpperCase());
		String redirectUrl = redirectServices.get(socialProvider).getRedirectUrl();

		response.sendRedirect(redirectUrl);

		return ResponseEntity.ok(ApiResponse.successNoContent(SUCCESS, "성공적으로 로그인 회원 참조 중입니다."));
	}

	@GetMapping("/{provider}/callback")
	public ResponseEntity<ApiResponse<Void>> handleOAuth2Callback(@PathVariable("provider") String provider,
		@RequestParam("code") String code, HttpServletResponse response) throws IOException {

		SocialProvider socialProvider = SocialProvider.valueOf(provider.toUpperCase());
		LoginResponse loginResponse = oAuthService.processOAuth2Login(code, socialProvider);

		ResponseCookie accessTokenCookie = ResponseCookie.from(ACCESS_TOKEN_NAME, loginResponse.accessToken())
			.maxAge(TimeUnit.MILLISECONDS.toSeconds(loginResponse.accessExpiredAt()))
			.path("/")
			.secure(true)
			.httpOnly(true)
			.sameSite("LAX")
			.build();

		response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
		response.sendRedirect("https://mcplink.co.kr");

		return ResponseEntity.ok(ApiResponse.successNoContent(SUCCESS, "성공적으로 로그인되었습니다."));
	}

	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
		oAuthService.logout(response);

		return ResponseEntity.ok(ApiResponse.successNoContent(SUCCESS, "성공적으로 로그아웃되었습니다."));
	}
}