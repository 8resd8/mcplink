package kr.co.mcplink.global.filter;

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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements Filter {

	private final JwtUtil jwtUtil;
	private final UserRepository userRepository;

	// 인증이 "반드시" 필요한 API 경로 목록
	// "/api" 접두사를 제외한 경로를 기입합니다. (application.yml의 server.servlet.context-path = /api 가정)
	private final List<String> securedApiPaths = Arrays.asList(
		"/contact" // 예: /api/contact 에 해당, 실제 API 경로로 수정 필요
		// "/posts" // 만약 게시글 작성/수정/삭제 등이 로그인 유저만 가능하다면 추가
	);

	// JWT 인증을 시도하지만, 필수는 아닌 경로들 (예: 로그인 상태면 개인화된 정보 제공)
	// 이 목록에 포함된 경로는 토큰이 있으면 유저 정보를 세팅하고, 없어도 통과시킴
	// securedApiPaths에 포함되지 않은 모든 경로는 여기에 해당한다고 볼 수 있음 (기본적으로 허용)

	// OAuth 관련 경로는 항상 토큰 검증 없이 통과되어야 함
	private final List<String> oAuthAndPublicPaths = Arrays.asList(
		"/auth/ssafy/login",
		"/auth/ssafy/callback",
		"/ssafy-login" // Redirect URI가 루트에 직접 매핑된 경우
		// Swagger 경로는 보통 개발/스테이징 환경에서만 필요하므로, 프로필 기반으로 추가하거나 제외 가능
		// "/swagger-ui/", "/v3/api-docs"
	);


	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		log.info("JwtAuthenticationFilter initialized.");
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
		throws IOException, ServletException {

		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		String requestURI = httpRequest.getRequestURI();

		// context-path (/api)를 제외한 실제 경로 추출
		String pathToCheck = requestURI;
		String contextPath = httpRequest.getContextPath(); // "/api"
		if (StringUtils.hasText(contextPath) && requestURI.startsWith(contextPath)) {
			pathToCheck = requestURI.substring(contextPath.length());
		}

		log.debug("Request URI: {}, Path to check for auth: {}", requestURI, pathToCheck);

		// OAuth 및 기타 명시적 공개 경로는 항상 통과
		if (isPathInList(pathToCheck, oAuthAndPublicPaths)) {
			log.debug("Path {} is an OAuth or explicitly public path, skipping JWT check.", pathToCheck);
			chain.doFilter(request, response);
			return;
		}

		// 그 외 Swagger UI 같은 개발용 공개 경로 처리 (필요시 활성화)
		if (pathToCheck.startsWith("/swagger-ui/") || pathToCheck.startsWith("/v3/api-docs")) {
		   log.debug("Path {} is a Swagger path, skipping JWT check.", pathToCheck);
		   chain.doFilter(request, response);
		   return;
		}


		String jwt = extractJwtFromRequest(httpRequest);
		User authenticatedUser = null;

		if (StringUtils.hasText(jwt) && jwtUtil.validateToken(jwt)) {
			try {
				String email = jwtUtil.getEmailFromToken(jwt);
				Optional<User> optionalUser = userRepository.findByEmail(email);
				if (optionalUser.isPresent()) {
					authenticatedUser = optionalUser.get();
					httpRequest.setAttribute("currentUser", authenticatedUser);
					log.debug("Authenticated user set in request: {} for path: {}", authenticatedUser.getEmail(), pathToCheck);
				} else {
					log.warn("User not found for email from token: {} (token was valid)", email);
					// 토큰은 유효하나 사용자가 DB에 없는 경우, securedApiPaths에 대한 접근은 막아야 함
				}
			} catch (Exception e) {
				log.error("Error processing JWT for path {}: {}", pathToCheck, e.getMessage());
				// JWT 파싱 오류 등. securedApiPaths에 대한 접근은 막아야 함
			}
		}

		// "인증이 반드시 필요한 경로"인지 확인
		if (isPathInList(pathToCheck, securedApiPaths)) {
			if (authenticatedUser == null) {
				log.warn("Access denied for path {}. Authentication required but no valid user found.", pathToCheck);
				httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
				httpResponse.setContentType("application/json");
				httpResponse.setCharacterEncoding("UTF-8");
				// 공통 ErrorResponse DTO가 있다면 사용, 없다면 간단한 JSON 메시지
				String errorJson = String.format("{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
					java.time.Instant.now().toString(),
					HttpStatus.UNAUTHORIZED.value(),
					HttpStatus.UNAUTHORIZED.getReasonPhrase(),
					"접근 권한이 없습니다. 로그인이 필요합니다.",
					requestURI);
				httpResponse.getWriter().write(errorJson);
				return; // 요청 처리 중단
			}
			// 인증된 사용자는 통과
			log.debug("Access granted for authenticated user {} to secured path {}", authenticatedUser.getEmail(), pathToCheck);
		} else {
			// "인증이 필수가 아닌 경로" (대부분의 경로)
			// 토큰이 있고 유효하면 authenticatedUser가 설정된 상태로 진행, 없거나 유효하지 않으면 null인 상태로 진행
			log.debug("Path {} does not require strict authentication. Proceeding (user may be null).", pathToCheck);
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

	private boolean isPathInList(String requestPath, List<String> pathsToCompare) {
		// AntPathMatcher를 사용하면 /posts/{postId}/** 같은 패턴 매칭도 가능합니다.
		// 여기서는 간단히 startsWith 또는 equals로 확인합니다.
		for (String path : pathsToCompare) {
			if (requestPath.equals(path) || (path.endsWith("/") && requestPath.startsWith(path)) || (!path.endsWith("/") && requestPath.startsWith(path + "/"))) {
				// 정확히 일치하거나, path가 /로 끝나고 requestPath가 해당 path로 시작하거나, path가 /로 끝나지 않고 requestPath가 path/로 시작하는 경우
				if (requestPath.equals(path)) return true; //  /contact 와 /contact 일치
				if (path.endsWith("/") && requestPath.startsWith(path)) return true; // /admin/ 와 /admin/users 일치
				//  /path 와 /path/abc 일치하도록 하려면 아래 조건 추가 (단, /pathabc 와는 구분)
				if (!path.endsWith("/") && requestPath.startsWith(path + "/")) return true;
			}
		}
		return false;
	}
}