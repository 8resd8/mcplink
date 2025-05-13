package kr.co.mcplink.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.co.mcplink.global.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Long id;

	@Column(name = "name")
	private String name;

	@Column(name = "nickname")
	private String nickname;

	@Builder
	private User(String name, String nickname) {
		this.name = name;
		this.nickname = nickname;
	}

	public static User createuser(String name, String nickname) {
		return User.builder()
			.name(name)
			.nickname(nickname)
			.build();
	}

}
