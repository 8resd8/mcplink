package kr.co.mcplink.domain.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePostRequest(
	@NotBlank(message = "제목은 필수 입력 항목입니다.")
	@Size(max = 100, message = "제목은 100자를 넘을 수 없습니다.")
	String title,

	@NotBlank(message = "내용은 필수 입력 항목입니다.")
	String content
) {
}