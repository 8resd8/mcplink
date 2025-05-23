package kr.co.mcplink.domain.user.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import kr.co.mcplink.global.common.BaseTimeEntity;
import kr.co.mcplink.global.common.SocialProvider;
import kr.co.mcplink.global.common.UserRole;
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

	@Column(name = "email", unique = true, nullable = false)
	private String email;

	@Column(name = "nickname")
	private String nickname;

	@Column(name = "role")
	@Enumerated(EnumType.STRING)
	private UserRole role;

	@Column(name = "last_login_at")
	private LocalDateTime lastLoginAt;

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<UserSocialAccount> socialAccounts = new ArrayList<>();

	@Builder
	private User(String name, String email, String nickname, UserRole role) {
		this.name = name;
		this.email = email;
		this.nickname = nickname;
		this.role = role;
	}

	public void updateLastLoginAt() {
		this.lastLoginAt = LocalDateTime.now();
	}

	public static User createUser(String name, String nickname,String email) {
		return User.builder()
			.name(name)
			.email(email)
			.nickname(nickname == null ? name : nickname)
			.role(UserRole.USER)
			.build();
	}

	public void addSocialAccount(SocialProvider provider, String providerId, String providerEmail) {
		UserSocialAccount socialAccount = UserSocialAccount.builder()
			.user(this)
			.provider(provider)
			.providerId(providerId)
			.providerEmail(providerEmail)
			.build();
		this.socialAccounts.add(socialAccount);
	}

}
