package kr.co.mcplink.domain.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.co.mcplink.domain.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

	// 이메일로 사용자 조회
	Optional<User> findByEmail(String email);

	// SSAFY User ID로 사용자 조회
	Optional<User> findBySsafyUserId(String ssafyUserId);
}
