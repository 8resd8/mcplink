package kr.co.mcplink.global.resolver;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import jakarta.servlet.http.HttpServletRequest;
import kr.co.mcplink.domain.user.entity.User;
import kr.co.mcplink.global.annotation.Login;
import kr.co.mcplink.global.exception.JwtForbiddenException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LoginArgumentResolver implements HandlerMethodArgumentResolver {

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
		User user = (User)request.getAttribute("user");

		if (user == null) {
			throw new JwtForbiddenException("여기서 문제생기면 안됨, 로그인이 필요한 서비스입니다. (@Login)");
		}

		return user;
	}
}