package kr.co.mcplink.domain.auth.ssafy.controller;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.web.server.Cookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import kr.co.mcplink.domain.auth.ssafy.dto.LoginResponse;
import kr.co.mcplink.domain.auth.ssafy.service.SsafyAuthService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/auth/ssafy")
@RequiredArgsConstructor
public class SsafyAuthController {

	private final SsafyAuthService ssafyAuthService;

	@GetMapping("/login")
	public void redirectToSsafyLogin(HttpServletResponse response) throws IOException {
		String redirect = ssafyAuthService.redirectToSsafyLogin();

		response.sendRedirect(redirect);
	}

	@GetMapping("/callback")
	public void handleSsafyCallback(@RequestParam("code") String code,
		HttpServletResponse httpServletResponse) throws IOException {
		LoginResponse response = ssafyAuthService.processSsafyLogin(code);

		ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", response.accessToken())
			.maxAge(TimeUnit.MILLISECONDS.toSeconds(response.accessExpiredAt()))
			.path("/")
			// .secure(true)
			.httpOnly(true)

			.sameSite(Cookie.SameSite.STRICT.name())
			.build();

		httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
		// httpServletResponse.sendRedirect("https://mcplink.co.kr");
		httpServletResponse.sendRedirect("http://localhost:5500");
	}
}