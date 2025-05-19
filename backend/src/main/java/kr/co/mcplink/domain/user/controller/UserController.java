package kr.co.mcplink.domain.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.co.mcplink.domain.user.dto.UserInfoDto;
import kr.co.mcplink.domain.user.entity.User;
import kr.co.mcplink.global.annotation.Login;
import kr.co.mcplink.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/v1/users") // 예시 경로
@RequiredArgsConstructor
public class UserController {

	@GetMapping("/info")
	public ResponseEntity<ApiResponse<UserInfoDto>> getMyInfo(@Login User user) {
		return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.toString(), "내 정보 조회 성공",
			new UserInfoDto(user.getId(), user.getName(), user.getEmail(), user.getNickname())));
	}
}