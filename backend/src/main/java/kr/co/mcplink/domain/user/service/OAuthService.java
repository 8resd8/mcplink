package kr.co.mcplink.domain.user.service;

import static kr.co.mcplink.global.common.Constants.*;

import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletResponse;
import kr.co.mcplink.domain.auth.dto.LoginResponse;
import kr.co.mcplink.domain.user.dto.OAuthUserInfo;
import kr.co.mcplink.domain.user.entity.User;
import kr.co.mcplink.domain.user.entity.UserSocialAccount;
import kr.co.mcplink.domain.user.repository.UserRepository;
import kr.co.mcplink.domain.user.repository.UserSocialAccountRepository;
import kr.co.mcplink.global.common.SocialProvider;
import kr.co.mcplink.global.config.JwtProperties;
import kr.co.mcplink.global.exception.ProviderNotFountException;
import kr.co.mcplink.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OAuthService {

	private final UserRepository userRepository;
	private final UserSocialAccountRepository socialAccountRepository;
	private final OauthSsafyService oauthSsafyService;
	private final JwtUtil jwtUtil;
	private final JwtProperties jwtProperties;

	public LoginResponse processOAuth2Login(String code, SocialProvider provider) {
		OAuthUserInfo userInfo = switch (provider) {
			case SSAFY -> oauthSsafyService.getUserInfo(code);
			// case GOOGLE -> googleOAuth2Service.getUserInfo(code);
			// case GITHUB -> githubOAuth2Service.getUserInfo(code);
			default -> throw new ProviderNotFountException("지원하지 않는 Provider입니다: " + provider);
		};

		User user = upsertUser(userInfo, provider);

		return new LoginResponse(jwtUtil.generateToken(user), jwtProperties.accessExpiration());
	}

	public void logout(HttpServletResponse response) {
		ResponseCookie deleteCookie = ResponseCookie.from(ACCESS_TOKEN_NAME, "")
			.maxAge(0)
			.path("/")
			.secure(true)
			.httpOnly(true)
			.sameSite("LAX")
			.build();
		response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
	}

	private User upsertUser(OAuthUserInfo userInfo, SocialProvider provider) {
		// 1. 소셜 계정으로 기존 연동 확인
		Optional<UserSocialAccount> socialAccount = socialAccountRepository
			.findByProviderAndProviderId(provider, userInfo.getProviderId());

		if (socialAccount.isPresent()) {
			User user = socialAccount.get().getUser();
			user.updateLastLoginAt();
			return userRepository.save(user);
		}

		// 2. 이메일로 기존 유저 확인
		Optional<User> existingUser = userRepository.findByEmail(userInfo.getEmail());

		if (existingUser.isPresent()) {
			User user = existingUser.get();
			addSocialAccount(user, provider, userInfo);
			user.updateLastLoginAt();
			return userRepository.save(user);
		}

		// 3. 신규 유저 생성
		User newUser = User.createUser(
			userInfo.getName(),
			userInfo.getName(),
			userInfo.getEmail());

		User savedUser = userRepository.save(newUser);
		addSocialAccount(savedUser, provider, userInfo);
		savedUser.updateLastLoginAt();

		return savedUser;
	}

	private void addSocialAccount(User user, SocialProvider provider, OAuthUserInfo userInfo) {
		user.addSocialAccount(provider, userInfo.getProviderId(), userInfo.getEmail());
	}
}