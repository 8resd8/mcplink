package kr.co.mcplink.global.exception;

public class CommentNotFoundException extends RuntimeException {
	public CommentNotFoundException(String message) {
		super(message);
	}
}
