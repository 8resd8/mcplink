package kr.co.mcplink.domain.comment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import kr.co.mcplink.domain.post.entity.Post;
import kr.co.mcplink.global.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "comments") // 테이블명 명시 (예: comments)
public class Comment extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "comment_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY) // 지연 로딩으로 설정하여 성능 최적화
	@JoinColumn(name = "post_id", nullable = false) // 외래키, null 불가
	private Post post;

	// @ManyToOne(fetch = FetchType.LAZY) // 사용자 엔티티와 연관관계 (현재는 주석 처리)
	// @JoinColumn(name = "user_id")
	// private User user;
	@Column(name = "user_id") // 임시로 사용자 ID 저장
	private Long userId;

	@Column(nullable = false, length = 1000) // 내용, null 불가, 길이 1000자 제한
	private String content;

	@Column(nullable = false, columnDefinition = "integer default 0") // 좋아요 수, null 불가, DB 기본값 0
	private Integer likeCount = 0;

	@Builder
	private Comment(Post post, String content, Long userId) {
		this.post = post;
		this.content = content;
		this.userId = userId;
		this.likeCount = 0;
	}

	public static Comment createComment(Post post, String content, Long userId) {
		return Comment.builder()
			.post(post)
			.content(content)
			.userId(userId)
			.build();
	}

	public void update(String content) {
		this.content = content;
	}

	public void incrementLikeCount() {
		this.likeCount++;
	}

	public void decrementLikeCount() {
		if (this.likeCount > 0) {
			this.likeCount--;
		}
	}
}