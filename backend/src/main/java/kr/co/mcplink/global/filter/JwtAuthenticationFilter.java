package kr.co.mcplink.global.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.mcplink.domain.user.entity.User;
import kr.co.mcplink.domain.user.repository.UserRepository;
import kr.co.mcplink.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component // Spring Bean으로 등록하여 의존성 주입을 받을 수 있도록 함
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements Filter {

	private final JwtUtil jwtUtil;
	private final UserRepository userRepository;

	// 인증이 필요 없는 경로 목록 (Swagger UI, OAuth 로그인/콜백 경로 등)
	// application.yml 등에서 관리하거나, 필요에 따라 확장 가능
	private final List<String> excludedPaths = Arrays.asList(
		"/swagger-ui/", "/v3/api-docs", // Swagger
		"/auth/ssafy/login", "/auth/ssafy/callback", // SSAFY OAuth 경로 (컨트롤러 매핑 기준)
		"/ssafy-login", // 만약 Redirect URI가 /ssafy-login 이고 별도 컨트롤러에서 처리한다면
		"/error" // 기본 에러 페이지
		// 기타 공개 API 경로 추가
	);

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		log.info("JwtAuthenticationFilter initialized.");
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
		throws IOException, ServletException {

		HttpServletRequest httpRequest = (HttpServletRequest)request;
		HttpServletResponse httpResponse = (HttpServletResponse)response;

		String requestURI = httpRequest.getRequestURI();

		// /api 접두사 제거 후 경로 비교 (server.servlet.context-path = /api 인 경우)
		String pathToCheck = requestURI.startsWith("/api") ? requestURI.substring(4) : requestURI;

		// 인증 예외 경로인지 확인
		if (isExcludedPath(pathToCheck)) {
			chain.doFilter(request, response);
			return;
		}

		String jwt = extractJwtFromRequest(httpRequest);

		if (StringUtils.hasText(jwt) && jwtUtil.validateToken(jwt)) {
			try {
				String email = jwtUtil.getEmailFromToken(jwt);
				Optional<User> optionalUser = userRepository.findByEmail(email);

				if (optionalUser.isPresent()) {
					User user = optionalUser.get();
					// 요청 객체에 사용자 정보 저장 (Controller 등에서 @RequestAttribute로 접근 가능)
					httpRequest.setAttribute("currentUser", user);
					log.debug("Authenticated user set in request: {}", user.getEmail());
				} else {
					log.warn("User not found for email from token: {}", email);
					// 사용자를 찾을 수 없는 경우, 토큰은 유효하나 해당 유저가 DB에 없을 때의 처리
					// 필요에 따라 여기서 401 Unauthorized 응답을 보낼 수 있음
					// httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not found");
					// return;
				}
			} catch (Exception e) {
				log.error("Error processing JWT: {}", e.getMessage());
				// JWT 파싱 또는 사용자 조회 중 예외 발생 시
				// httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token details");
				// return;
			}
		} else {
			log.debug("No valid JWT found for protected path: {}", requestURI);
			// 유효한 JWT가 없는 경우 (보호된 경로에 대해)
			// 필요에 따라 여기서 401 Unauthorized 응답을 보낼 수 있음
			// httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
			// return;
		}

		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
		log.info("JwtAuthenticationFilter destroyed.");
	}

	private String extractJwtFromRequest(HttpServletRequest request) {
		String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}
		return null;
	}

	private boolean isExcludedPath(String requestPath) {
		// 참고: AntPathMatcher 등을 사용하면 더 유연한 경로 매칭 가능
		return excludedPaths.stream().anyMatch(excludedPath -> requestPath.startsWith(excludedPath));
	}
}