package kr.co.mcplink.domain.auth.ssafy.controller;

import static kr.co.mcplink.global.common.Constants.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import kr.co.mcplink.domain.auth.ssafy.dto.LoginResponse;
import kr.co.mcplink.domain.auth.ssafy.service.SsafyAuthService;
import kr.co.mcplink.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class SsafyAuthController {

	private final SsafyAuthService ssafyAuthService;

	@GetMapping("/ssafy/login")
	public void redirectToSsafyLogin(HttpServletResponse response) throws IOException {
		String redirect = ssafyAuthService.redirectToSsafyLogin();

		response.sendRedirect(redirect);
	}

	@GetMapping("/ssafy/callback")
	public void handleSsafyCallback(@RequestParam("code") String code,
		HttpServletResponse httpServletResponse) throws IOException {
		LoginResponse response = ssafyAuthService.processSsafyLogin(code);

		ResponseCookie accessTokenCookie = ResponseCookie.from(ACCESS_TOKEN_NAME, response.accessToken())
			.maxAge(TimeUnit.MILLISECONDS.toSeconds(response.accessExpiredAt()))
			.path("/")
			.secure(true)
			.httpOnly(true)
			.sameSite("LAX")
			.build();

		httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
		httpServletResponse.sendRedirect("https://mcplink.co.kr");
	}

	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
		ssafyAuthService.logout(response);

		return ResponseEntity.ok(ApiResponse.successNoContent(SUCCESS, "성공적으로 로그아웃되었습니다."));
	}
}