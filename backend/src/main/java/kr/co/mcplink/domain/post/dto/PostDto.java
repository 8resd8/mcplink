package kr.co.mcplink.domain.post.dto;

import java.time.LocalDateTime;

import kr.co.mcplink.domain.post.entity.Post;

public record PostDto(
	Long postId,
	String creator,
	String title,
	String content,
	Integer likeCount,
	LocalDateTime createAt,
	LocalDateTime updateAt
) {
	public static PostDto of(Post post) {
		return new PostDto(
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