package kr.co.mcplink.domain.post.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kr.co.mcplink.domain.post.entity.Post;
import kr.co.mcplink.domain.user.entity.User;

public interface PostRepository extends JpaRepository<Post, Long> {

	@Query("select p from Post p join fetch p.user where p.id = :postId and p.user = :user")
	Optional<Post> findByIdAndUser(@Param("postId") Long postId, @Param("user") User user);

	Optional<Post> findPostWithUserById(Long postId);

	@Query("select distinct p from Post p join fetch p.user")
	List<Post> findAllWithUser();

}