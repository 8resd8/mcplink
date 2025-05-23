package kr.co.mcplink.global.exception;

public class JwtForbiddenException extends RuntimeException {

	public JwtForbiddenException(String message, Throwable cause) {
		super(message);
	}

	public JwtForbiddenException(String message) {
		super(message);
	}

	public JwtForbiddenException() {
		super("접근이 금지된 경로입니다.");
	}
}
