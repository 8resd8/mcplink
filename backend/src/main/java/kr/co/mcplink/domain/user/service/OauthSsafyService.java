package kr.co.mcplink.domain.user.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import kr.co.mcplink.domain.auth.ssafy.dto.OAuthSsafyUserInfo;
import kr.co.mcplink.domain.auth.ssafy.dto.SsafyTokenDto;
import kr.co.mcplink.domain.auth.ssafy.dto.SsafyUserInfoDto;
import kr.co.mcplink.domain.user.dto.OAuthUserInfo;
import kr.co.mcplink.domain.user.entity.User;
import kr.co.mcplink.domain.user.repository.UserRepository;
import kr.co.mcplink.global.config.SsafyOauthProperties;
import kr.co.mcplink.global.exception.JwtForbiddenException;
import kr.co.mcplink.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OauthSsafyService {

	private final UserRepository userRepository;
	private final RestTemplate restTemplate;
	private final SsafyOauthProperties ssafyProperties;
	private final JwtUtil jwtUtil;

	public OAuthUserInfo getUserInfo(String code) {
		SsafyTokenDto tokenDto = requestAccessToken(code);
		validateToken(tokenDto);

		SsafyUserInfoDto userInfo = requestUserInfo(tokenDto.accessToken());

		return new OAuthSsafyUserInfo(userInfo);
	}

	private SsafyUserInfoDto requestUserInfo(String accessToken) {
		Long userId = jwtUtil.getUserIdFromToken(accessToken);
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new JwtForbiddenException("SSAFY 사용자 정보를 조회하는데 실패했습니다."));

		return new SsafyUserInfoDto(user.getSsafyUserId(), user.getEmail(), user.getName());
	}

	private SsafyUserInfoDto validateToken(SsafyTokenDto ssafyTokenDto) {
		if (ssafyTokenDto == null) {
			throw new JwtForbiddenException("SSAFY Access Token을 발급받는데 실패했습니다.");
		}

		SsafyUserInfoDto ssafyUserInfo = requestUserInfo(ssafyTokenDto.accessToken());

		if (ssafyUserInfo.email() == null) {
			throw new JwtForbiddenException("SSAFY 사용자 정보를 조회하는데 실패했습니다.");
		}
		return ssafyUserInfo;
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
			ResponseEntity<SsafyTokenDto> response = restTemplate.postForEntity(
				ssafyProperties.tokenUri(), request, SsafyTokenDto.class);
			return response.getBody();
		} catch (HttpClientErrorException e) {
			throw new HttpClientErrorException(e.getStatusCode(), e.getMessage());
		}
	}
}