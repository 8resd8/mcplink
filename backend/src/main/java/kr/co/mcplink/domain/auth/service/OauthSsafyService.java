package kr.co.mcplink.domain.auth.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import kr.co.mcplink.domain.auth.dto.OAuthSsafyUserInfo;
import kr.co.mcplink.domain.auth.dto.SsafyTokenDto;
import kr.co.mcplink.domain.auth.dto.SsafyUserInfoDto;
import kr.co.mcplink.domain.user.dto.OAuthUserInfo;
import kr.co.mcplink.global.config.SsafyOauthProperties;
import kr.co.mcplink.global.exception.JwtForbiddenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OauthSsafyService {

	private final RestTemplate restTemplate;
	private final SsafyOauthProperties ssafyProperties;

	public OAuthUserInfo getUserInfo(String code) {
		SsafyTokenDto tokenDto = requestAccessToken(code);

		SsafyUserInfoDto userInfoFromApi = requestUserInfoFromSsafy(tokenDto.accessToken());

		return new OAuthSsafyUserInfo(userInfoFromApi);
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

	private SsafyUserInfoDto requestUserInfoFromSsafy(String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

		try {
			ResponseEntity<SsafyUserInfoDto> response = restTemplate.exchange(
				ssafyProperties.userInfoUri(),
				org.springframework.http.HttpMethod.GET,
				requestEntity,
				SsafyUserInfoDto.class
			);

			return response.getBody();
		} catch (HttpClientErrorException e) {
			log.error("SSAFY 사용자 정보 요청 실패: {}, 응답: {}", e.getMessage(), e.getResponseBodyAsString());
			throw new JwtForbiddenException("SSAFY 사용자 정보를 조회하는데 실패했습니다. (SSAFY API 오류)", e);
		}
	}

}