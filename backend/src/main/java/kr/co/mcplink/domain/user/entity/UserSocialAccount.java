package kr.co.mcplink.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kr.co.mcplink.global.common.BaseTimeEntity;
import kr.co.mcplink.global.common.SocialProvider;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_social_accounts",
	uniqueConstraints = {
		@UniqueConstraint(columnNames = {"provider", "provider_id"})
	})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSocialAccount extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "social_account_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "provider", nullable = false)
	@Enumerated(EnumType.STRING)
	private SocialProvider provider;

	@Column(name = "provider_id", nullable = false)
	private String providerId;

	@Column(name = "provider_email")
	private String providerEmail;

	@Builder
	private UserSocialAccount(User user, SocialProvider provider, String providerId,
		String providerEmail) {
		this.user = user;
		this.provider = provider;
		this.providerId = providerId;
		this.providerEmail = providerEmail;
	}

	public static UserSocialAccount createUserSocialAccount(User user, SocialProvider provider, String providerId,
		String providerEmail) {
		return UserSocialAccount.builder()
			.user(user)
			.provider(provider)
			.providerId(providerId)
			.providerEmail(providerEmail)
			.build();
	}
}