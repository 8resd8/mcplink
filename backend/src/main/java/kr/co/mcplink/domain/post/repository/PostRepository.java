package kr.co.mcplink.domain.post.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.co.mcplink.domain.post.entity.Post;
import kr.co.mcplink.domain.user.entity.User;

public interface PostRepository extends JpaRepository<Post, Long> {

	Optional<Post> findByIdAndUser(Long postId, User user);
}