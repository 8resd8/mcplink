package kr.co.mcplink.domain.auth.ssafy.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import kr.co.mcplink.domain.auth.ssafy.dto.LoginResponse;
import kr.co.mcplink.domain.auth.ssafy.service.SsafyAuthService;
import kr.co.mcplink.global.common.ApiResponse;
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
	public ResponseEntity<ApiResponse<LoginResponse>> handleSsafyCallback(@RequestParam("code") String code) {
		LoginResponse response = ssafyAuthService.processSsafyLogin(code);

		return ResponseEntity.
			status(HttpStatus.OK)
			.body(ApiResponse.success(HttpStatus.OK.toString(), "SSAFY 로그인 성공", response));
	}
}