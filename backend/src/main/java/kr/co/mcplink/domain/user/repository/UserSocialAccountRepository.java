package kr.co.mcplink.domain.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.co.mcplink.domain.user.entity.UserSocialAccount;
import kr.co.mcplink.global.common.SocialProvider;

public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, Long> {

	Optional<UserSocialAccount> findByProviderAndProviderId(SocialProvider provider, String providerId);

	Optional<UserSocialAccount> findByUserId(Long userId);
}