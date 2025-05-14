package kr.co.mcplink.domain.post.dto;

import java.time.LocalDateTime;

import kr.co.mcplink.domain.post.entity.Post;

public record PostDetailDto(
	Long postId,
	String creator,
	String title,
	String content,
	Integer likeCount,
	LocalDateTime createAt,
	LocalDateTime updateAt
) {
	public static PostDetailDto of(Post post) {
		return new PostDetailDto(
			post.getId(),
			post.getUser().getName(),
			post.getTitle(),
			post.getContent(),
			post.getLikeCount(),
			post.getCreatedAt(),
			post.getUpdatedAt()
		);
	}
}