package kr.co.mcplink.global.exception;

public class UserNotFoundException extends RuntimeException {
	public UserNotFoundException(String message) {
		super(message);
	}

	public UserNotFoundException() {
		super("User not found");
	}
}
