package kr.co.mcplink.global.exception.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import kr.co.mcplink.global.common.ErrorResponse;
import kr.co.mcplink.global.exception.CommentNotFoundException;
import kr.co.mcplink.global.exception.JwtForbiddenException;
import kr.co.mcplink.global.exception.PostNotFoundException;
import kr.co.mcplink.global.exception.UserNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler
	public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {
		return ErrorResponse.toResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR,
			"서버 오류가 발생했습니다. 계속해서 문제가 발생하면 관리자에게 문의해주세요.",
			request.getRequestURI());
	}

	@ExceptionHandler
	public ResponseEntity<ErrorResponse> handleMethodArgumentException(MethodArgumentNotValidException ex,
		HttpServletRequest request) {
		return ErrorResponse.toResponseEntity(HttpStatus.BAD_REQUEST, "요청 데이터가 올바르지 않습니다.", request.getRequestURI());
	}

	@ExceptionHandler
	public ResponseEntity<ErrorResponse> handleNoResourceException(NoResourceFoundException ex,
		HttpServletRequest request) {
		return ErrorResponse.toResponseEntity(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다.", request.getRequestURI());
	}

	@ExceptionHandler
	public ResponseEntity<ErrorResponse> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException ex,
		HttpServletRequest request) {
		return ErrorResponse.toResponseEntity(HttpStatus.METHOD_NOT_ALLOWED, "해당 요청 방식은 지원하지 않습니다.",
			request.getRequestURI());
	}

	@ExceptionHandler
	public ResponseEntity<ErrorResponse> handleMessageNotReadableException(HttpMessageNotReadableException ex,
		HttpServletRequest request) {
		return ErrorResponse.toResponseEntity(HttpStatus.BAD_REQUEST, "정확한 타입으로 보내야 합니다.", request.getRequestURI());
	}

	@ExceptionHandler
	public ResponseEntity<ErrorResponse> handleNotFoundPostException(PostNotFoundException ex,
		HttpServletRequest request) {
		return ErrorResponse.toResponseEntity(HttpStatus.NO_CONTENT, "게시글이 없습니다.", request.getRequestURI());
	}

	@ExceptionHandler
	public ResponseEntity<ErrorResponse> handleNotFoundCommentException(CommentNotFoundException ex,
		HttpServletRequest request) {
		return ErrorResponse.toResponseEntity(HttpStatus.NO_CONTENT, "댓글이 없습니다.", request.getRequestURI());
	}

	@ExceptionHandler
	public ResponseEntity<ErrorResponse> handleForbbidenException(JwtForbiddenException ex,
		HttpServletRequest request) {
		return ErrorResponse.toResponseEntity(HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI());
	}

	@ExceptionHandler
	public ResponseEntity<ErrorResponse> handleNotFoundUserException(UserNotFoundException ex,
		HttpServletRequest request) {
		return ErrorResponse.toResponseEntity(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다.", request.getRequestURI());
	}

	@ExceptionHandler
	public ResponseEntity<ErrorResponse> handleHttpClientException(HttpClientErrorException ex,
		HttpServletRequest request) {
		return ErrorResponse.toResponseEntity(HttpStatus.UNAUTHORIZED, "SSAFY 통신 에러", request.getRequestURI());
	}

	

}
