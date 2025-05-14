package kr.co.mcplink.domain.auth.ssafy.service;


import kr.co.mcplink.domain.auth.ssafy.dto.SaafyTokenDto;
import kr.co.mcplink.domain.auth.ssafy.dto.SsafyUserInfoDto;
import kr.co.mcplink.domain.user.entity.User;
import kr.co.mcplink.domain.user.repository.UserRepository;
import kr.co.mcplink.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SsafyAuthService {

	private final UserRepository userRepository;
	private final JwtUtil jwtUtil;
	private final RestTemplate restTemplate; // RestTemplate 빈 주입

	@Value("${ssafy.oauth.client-id}")
	private String clientId;

	@Value("${ssafy.oauth.client-secret}")
	private String clientSecret;

	@Value("${ssafy.oauth.redirect-uri}")
	private String redirectUri;

	@Value("${ssafy.oauth.token-uri}")
	private String tokenUri;

	@Value("${ssafy.oauth.user-info-uri}")
	private String userInfoUri;

	@Transactional
	public String processSsafyLogin(String code) {
		// 1. 인가 코드로 AccessToken 요청
		SaafyTokenDto tokenResponse = requestAccessToken(code);
		if (tokenResponse == null || tokenResponse.accessToken() == null) {
			log.error("SSAFY Access Token 발급 실패");
			// TODO: 적절한 예외 처리
			throw new RuntimeException("SSAFY Access Token을 발급받는데 실패했습니다.");
		}

		// 2. AccessToken으로 사용자 정보 요청
		SsafyUserInfoDto ssafyUserInfo = requestUserInfo(tokenResponse.accessToken());
		if (ssafyUserInfo == null || ssafyUserInfo.email() == null) {
			log.error("SSAFY 사용자 정보 조회 실패");
			// TODO: 적절한 예외 처리
			throw new RuntimeException("SSAFY 사용자 정보를 조회하는데 실패했습니다.");
		}

		// 3. 사용자 정보로 회원가입 또는 로그인 처리
		User user = upsertUser(ssafyUserInfo);

		// 4. JWT 생성
		return jwtUtil.generateToken(user);
	}

	private SaafyTokenDto requestAccessToken(String code) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("grant_type", "authorization_code");
		params.add("client_id", clientId);
		params.add("client_secret", clientSecret);
		params.add("redirect_uri", redirectUri);
		params.add("code", code);

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

		try {
			ResponseEntity<SaafyTokenDto> response = restTemplate.postForEntity(
				tokenUri, request, SaafyTokenDto.class);
			log.info("SSAFY 토큰 발급 응답: {}", response.getBody());
			return response.getBody();
		} catch (HttpClientErrorException e) {
			log.error("SSAFY 토큰 발급 요청 실패: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
			// TODO: 상세 예외 처리 (예: 4xx, 5xx 에러 구분)
			return null;
		} catch (Exception e) {
			log.error("SSAFY 토큰 발급 중 알 수 없는 오류: {}", e.getMessage());
			return null;
		}
	}

	private SsafyUserInfoDto requestUserInfo(String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken); // "Authorization: Bearer {accessToken}"
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED); // SSAFY 가이드에 따름

		HttpEntity<Void> request = new HttpEntity<>(headers);

		try {
			ResponseEntity<SsafyUserInfoDto> response = restTemplate.exchange(
				userInfoUri, HttpMethod.GET, request, SsafyUserInfoDto.class);
			log.info("SSAFY 사용자 정보 조회 응답: {}", response.getBody());
			return response.getBody();
		} catch (HttpClientErrorException e) {
			log.error("SSAFY 사용자 정보 요청 실패: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
			return null;
		} catch (Exception e) {
			log.error("SSAFY 사용자 정보 조회 중 알 수 없는 오류: {}", e.getMessage());
			return null;
		}
	}

	private User upsertUser(SsafyUserInfoDto ssafyUserInfo) {
		// SSAFY 고유 ID (userId) 또는 이메일로 기존 사용자 검색
		// 가이드 상 userId가 고유 식별 번호이므로, ssafyUserInfo.ssafyUserId()를 우선적으로 사용하는 것이 좋음
		// 여기서는 email을 우선으로 사용 (User 엔티티에 email이 unique로 설정되어 있기 때문)
		Optional<User> optionalUser = userRepository.findByEmail(ssafyUserInfo.email());

		User user;
		if (optionalUser.isPresent()) {
			// 기존 사용자 정보 업데이트 (예: 이름, SSAFY ID 등)
			user = optionalUser.get();
			user.updateSsafyUser(ssafyUserInfo.name(), ssafyUserInfo.ssafyUserId());
		} else {
			// 신규 사용자 생성
			user = User.createSsafyUser(
				ssafyUserInfo.name(),
				ssafyUserInfo.email(),
				ssafyUserInfo.ssafyUserId()
			);
		}
		return userRepository.save(user);
	}
}