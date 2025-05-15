package kr.co.mcplink.domain.auth.ssafy.service;

import java.util.Optional;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import kr.co.mcplink.domain.auth.ssafy.dto.LoginResponse;
import kr.co.mcplink.domain.auth.ssafy.dto.SsafyTokenDto;
import kr.co.mcplink.domain.auth.ssafy.dto.SsafyUserInfoDto;
import kr.co.mcplink.domain.user.entity.User;
import kr.co.mcplink.domain.user.repository.UserRepository;
import kr.co.mcplink.global.config.JwtProperties;
import kr.co.mcplink.global.config.SsafyOauthProperties;
import kr.co.mcplink.global.exception.JwtForbiddenException;
import kr.co.mcplink.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SsafyAuthService {

	private final UserRepository userRepository;
	private final JwtUtil jwtUtil;
	private final RestTemplate restTemplate;
	private final SsafyOauthProperties ssafyProperties;
	private final JwtProperties jwtProperties;

	public LoginResponse processSsafyLogin(String code) {
		SsafyTokenDto ssafyTokenDto = requestAccessToken(code);
		SsafyUserInfoDto ssafyUserInfo = validateDto(ssafyTokenDto);

		User user = upsertUser(ssafyUserInfo);

		return new LoginResponse(jwtUtil.generateToken(user), jwtProperties.accessExpiration());
	}

	private SsafyUserInfoDto validateDto(SsafyTokenDto ssafyTokenDto) {
		if (ssafyTokenDto == null || ssafyTokenDto.accessToken() == null) {
			throw new JwtForbiddenException("SSAFY Access Token을 발급받는데 실패했습니다.");
		}

		SsafyUserInfoDto ssafyUserInfo = requestUserInfo(ssafyTokenDto.accessToken());
		if (ssafyUserInfo == null || ssafyUserInfo.email() == null) {
			throw new JwtForbiddenException("SSAFY 사용자 정보를 조회하는데 실패했습니다.");
		}
		return ssafyUserInfo;
	}

	public String redirectToSsafyLogin() {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(ssafyProperties.authorizationUri())
			.queryParam("response_type", "code")
			.queryParam("client_id", ssafyProperties.clientId())
			.queryParam("redirect_uri", ssafyProperties.redirectUri())
			.queryParam("scope", ssafyProperties.scope().replace(",", " "));

		return builder.toUriString();
	}

	private SsafyTokenDto requestAccessToken(String code) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("grant_type", "authorization_code");
		params.add("client_id", ssafyProperties.clientId());
		params.add("client_secret", ssafyProperties.clientSecret());
		params.add("redirect_uri", ssafyProperties.redirectUri());
		params.add("code", code);

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
		try {
			ResponseEntity<SsafyTokenDto> response = restTemplate.postForEntity(ssafyProperties.tokenUri(), request,
				SsafyTokenDto.class);
			return response.getBody();
		} catch (Exception e) {
			log.error("SSAFY Access Token을 발급받는데 실패했습니다.");
			throw new JwtForbiddenException("SSAFY Access Token을 발급받는데 실패했습니다.");
		}
		// return response.getBody();
	}

	private SsafyUserInfoDto requestUserInfo(String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		HttpEntity<Void> request = new HttpEntity<>(headers);

		ResponseEntity<SsafyUserInfoDto> response = restTemplate.exchange(
			ssafyProperties.userInfoUri(),
			HttpMethod.GET,
			request,
			SsafyUserInfoDto.class);

		return response.getBody();
	}

	private User upsertUser(SsafyUserInfoDto ssafyUserInfo) {
		Optional<User> optionalUser = userRepository.findByEmail(ssafyUserInfo.email());

		User user;
		if (optionalUser.isPresent()) {
			user = optionalUser.get();
			user.updateSsafyUser(ssafyUserInfo.name(), ssafyUserInfo.ssafyUserId());
		} else {
			user = User.createSsafyUser(
				ssafyUserInfo.name(),
				ssafyUserInfo.email(),
				ssafyUserInfo.ssafyUserId()
			);
		}

		return userRepository.save(user);
	}
}