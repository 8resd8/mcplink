package kr.co.mcplink.global.resolver;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import jakarta.servlet.http.HttpServletRequest;
import kr.co.mcplink.domain.user.entity.User;
import kr.co.mcplink.domain.user.repository.UserRepository;
import kr.co.mcplink.global.annotation.Login;
import kr.co.mcplink.global.exception.JwtForbiddenException;
import kr.co.mcplink.global.exception.UserNotFoundException;
import kr.co.mcplink.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LoginArgumentResolver implements HandlerMethodArgumentResolver {

	private final JwtUtil jwtUtil;
	private final UserRepository userRepository;

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		boolean hasLoginAnnotation = parameter.getParameterAnnotation(Login.class) != null;
		boolean hasUserType = User.class.isAssignableFrom(parameter.getParameterType());

		return hasLoginAnnotation && hasUserType;
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
		NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		HttpServletRequest request = (HttpServletRequest)webRequest.getNativeRequest();
		// User user = (User)request.getAttribute("user");
		String token = jwtUtil.getResolveAccessToken(request);

		if (token == null) {
			throw new JwtForbiddenException("Token is Null");
		}

		Long userId = jwtUtil.getUserIdFromToken(token);

		return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("문제있으면 안되는 곳, User Not Found"));
	}
}