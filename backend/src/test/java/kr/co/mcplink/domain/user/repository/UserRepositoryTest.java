package kr.co.mcplink.domain.user.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.EntityManager;
import kr.co.mcplink.domain.user.entity.User;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

	@Autowired
	UserRepository userRepository;

	@Autowired
	EntityManager em;

	@Test
	void 사용자저장_조회_테스트() {
		String userName = "resd";
		String nickname = "resd-super-resd";

		User user = User.createUser(userName, nickname);

		User savedUser = userRepository.save(user);

		em.flush();
		em.clear();

		Optional<User> foundUserOptional = userRepository.findById(savedUser.getId());

		assertThat(foundUserOptional).isPresent();
		User findUser = foundUserOptional.get();

		assertThat(findUser.getName()).isEqualTo(userName);
		assertThat(findUser.getNickname()).isEqualTo(nickname);
		assertThat(findUser.getCreatedAt()).isNotNull();
		assertThat(findUser.getUpdatedAt()).isNotNull();
		assertThat(findUser.getId()).isEqualTo(savedUser.getId());
	}
}