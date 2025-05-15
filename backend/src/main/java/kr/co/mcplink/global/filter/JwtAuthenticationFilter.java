package kr.co.mcplink.global.filter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import kr.co.mcplink.domain.user.entity.User;
import kr.co.mcplink.domain.user.repository.UserRepository;
import kr.co.mcplink.global.config.JwtProperties;
import kr.co.mcplink.global.exception.JwtForbiddenException;
import kr.co.mcplink.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements Filter {

	private final JwtUtil jwtUtil;
	private final UserRepository userRepository;
	private final JwtProperties jwtProperties;
	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
		throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest)request;
		String requestURI = httpRequest.getRequestURI();
		String httpMethod = httpRequest.getMethod();

		String pathToCheck = requestURI;
		String contextPath = httpRequest.getContextPath(); // /api
		if (StringUtils.hasText(contextPath) && requestURI.startsWith(contextPath)) {
			pathToCheck = requestURI.substring(contextPath.length());
		}

		// 항상 통과 경로
		if (isPathMatch(pathToCheck, jwtProperties.oauthSsafyPath())) {
			chain.doFilter(request, response);
			return;
		}

		String jwt = extractJwtFromRequest(httpRequest);
		User authenticatedUser = null;

		if (StringUtils.hasText(jwt) && jwtUtil.validateToken(jwt)) {
			String email = jwtUtil.getEmailFromToken(jwt);
			Optional<User> optionalUser = userRepository.findByEmail(email);

			if (optionalUser.isPresent()) {
				authenticatedUser = optionalUser.get();
				httpRequest.setAttribute("user", authenticatedUser);
			}
		}

		// 보안 경로 검사
		if (isSecurePathRequested(pathToCheck, httpMethod, jwtProperties.securePath()) && authenticatedUser == null) {
			throw new JwtForbiddenException("로그인이 필요한 서비스입니다.");
		}

		chain.doFilter(request, response);
	}

	private String extractJwtFromRequest(HttpServletRequest request) {
		String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}
		return null;
	}

	private boolean isPathMatch(String requestPath, List<String> patterns) {
		if (patterns == null) {
			return false;
		}

		for (String pattern : patterns) {
			if (pathMatcher.match(pattern, requestPath)) {
				return true;
			}
		}
		return false;
	}

	// 보안 경로 및 HTTP 메서드 검사 로직
	private boolean isSecurePathRequested(String requestPath, String httpMethod, List<String> securePathPatterns) {
		if (securePathPatterns == null) {
			return false;
		}

		for (String pattern : securePathPatterns) {
			if (pathMatcher.match(pattern, requestPath)) {
				if (pattern.equals("/v1/posts") && HttpMethod.POST.name().equalsIgnoreCase(httpMethod)) {
					return true;
				} else if (pattern.equals("/v1/posts/**") &&
					(HttpMethod.PUT.name().equalsIgnoreCase(httpMethod) || HttpMethod.DELETE.name()
						.equalsIgnoreCase(httpMethod))) {
					return true;
				}
			}
		}
		return false;
	}
}