package kr.co.mcplink.domain.post.service;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import kr.co.mcplink.domain.post.dto.PostDto;
import kr.co.mcplink.domain.post.dto.request.CreatePostRequest;
import kr.co.mcplink.domain.post.dto.request.UpdatePostRequest;
import kr.co.mcplink.domain.post.dto.response.PostResponse;
import kr.co.mcplink.domain.post.entity.Post;
import kr.co.mcplink.domain.post.repository.PostRepository;
import kr.co.mcplink.domain.user.entity.User;
import kr.co.mcplink.domain.user.repository.UserRepository;
import kr.co.mcplink.global.exception.PostNotFoundException;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class PostServiceTest {

	@Autowired
	private PostService postService;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager em;

	private User fixedUser;
	private Post post;

	@BeforeEach
	void setUp() {
		postRepository.deleteAll();
		userRepository.deleteAll();

		User userToSave = User.createUser("fixedUser", "fixed_user");
		fixedUser = userRepository.save(userToSave);

		if (!userRepository.existsById(1L)) {
			userRepository.deleteAll();
			User userWithIdOne = User.createUser("fixedUser", "fixed_user");
			fixedUser = userRepository.save(userWithIdOne);
		} else {
			fixedUser = userRepository.findById(1L)
				.orElseThrow(() -> new RuntimeException("Test setup failed: User with ID 1L not found"));
		}

		post = Post.createPost("원본 제목", "원본 내용", fixedUser);
		postRepository.save(post);
	}

	@Test
	@DisplayName("게시글 생성")
	void 게시글_생성() {
		CreatePostRequest request = new CreatePostRequest("새 게시글 제목", "새 게시글 내용");

		postService.createPost(request, fixedUser);

		List<Post> posts = postRepository.findAll();
		assertThat(posts).hasSize(2); // beforeEach 기본 1개저장

		Post savedPost = posts.get(1);

		assertThat(post.getTitle()).isEqualTo("원본 제목");
		assertThat(post.getContent()).isEqualTo("원본 내용");

		assertThat(savedPost.getTitle()).isEqualTo("새 게시글 제목");
		assertThat(savedPost.getContent()).isEqualTo("새 게시글 내용");

		assertThat(savedPost.getUser().getId()).isEqualTo(fixedUser.getId());
		assertThat(savedPost.getLikeCount()).isZero();
	}

	@Test
	@DisplayName("게시글 전체 조회")
	void 게시글_전체_조회() {
		postRepository.save(Post.createPost("제목1", "내용1", fixedUser));
		postRepository.save(Post.createPost("제목2", "내용2", fixedUser));

		List<PostDto> postDtos = postService.getAllPosts();

		assertThat(postDtos).hasSize(3);
		assertThat(postDtos.get(0).title()).isEqualTo("원본 제목");
		assertThat(postDtos.get(1).title()).isEqualTo("제목1");
		assertThat(postDtos.get(2).title()).isEqualTo("제목2");
	}

	@Test
	@DisplayName("특정 게시글 ID로 조회")
	void 특정_게시글_조회() {
		Post saved = postRepository.save(Post.createPost("특정 게시글", "내용", fixedUser));

		PostResponse response = postService.getPostById(saved.getId());
		PostDto postDto = response.postDetail();
		assertThat(postDto).isNotNull();
		assertThat(postDto.postId()).isEqualTo(saved.getId());
		assertThat(postDto.title()).isEqualTo("특정 게시글");
	}

	@Test
	@DisplayName("존재하지 않는 게시글 ID로 조회 시 PostNotFoundException 발생")
	void 존재하지_않는_게시글_조회_예외() {
		Long nonExistentPostId = 999L;

		assertThatThrownBy(() -> postService.getPostById(nonExistentPostId))
			.isInstanceOf(PostNotFoundException.class)
			.hasMessageContaining("Post not found");
	}

	@Test
	@DisplayName("게시글 수정")
	void 게시글_수정() {
		UpdatePostRequest request = new UpdatePostRequest("수정된 제목", "수정된 내용");

		PostDto updatedPostDto = postService.updatePost(post.getId(), request, fixedUser);

		assertThat(updatedPostDto).isNotNull();
		assertThat(updatedPostDto.postId()).isEqualTo(post.getId());
		assertThat(updatedPostDto.title()).isEqualTo("수정된 제목");
		assertThat(updatedPostDto.content()).isEqualTo("수정된 내용");

		Post updatedPostInDb = postRepository.findByIdAndUser(post.getId(), fixedUser).orElseThrow();

		assertThat(updatedPostInDb.getTitle()).isEqualTo("수정된 제목");
		assertThat(updatedPostInDb.getContent()).isEqualTo("수정된 내용");
	}

	@Test
	@DisplayName("게시글 수정 시 제목만 null이면 기존 제목 유지")
	void 게시글_수정_제목만_null() {
		UpdatePostRequest request = new UpdatePostRequest(null, "수정된 내용");

		PostDto updatedPostDto = postService.updatePost(post.getId(), request, fixedUser);

		assertThat(updatedPostDto.title()).isEqualTo("원본 제목");
		assertThat(updatedPostDto.content()).isEqualTo("수정된 내용");
	}

	@Test
	@DisplayName("게시글 수정 시 내용만 null이면 기존 내용 유지")
	void 게시글_수정_내용만_null() {
		Post saved = postRepository.save(Post.builder().title("원본 제목").content("원본 내용").user(fixedUser).build());
		UpdatePostRequest request = new UpdatePostRequest("수정된 제목", null); // 내용을 null로

		PostDto updatedPostDto = postService.updatePost(saved.getId(), request, fixedUser);

		assertThat(updatedPostDto.title()).isEqualTo("수정된 제목");
		assertThat(updatedPostDto.content()).isEqualTo("원본 내용"); // Post 엔티티의 updatePost 로직에 따름
	}

	@Test
	@DisplayName("존재하지 않는 게시글 수정 시 PostNotFoundException 발생")
	void 존재하지_않는_게시글_수정_예외() {
		Long nonExistentPostId = 999L;
		UpdatePostRequest request = new UpdatePostRequest("수정 시도", "내용 시도");

		assertThatThrownBy(() -> postService.updatePost(nonExistentPostId, request, fixedUser))
			.isInstanceOf(PostNotFoundException.class);
	}

	@Test
	@DisplayName("게시글 삭제")
	void 게시글_삭제() {
		Post saved = postRepository.save(Post.createPost("삭제될 게시글", "내용", fixedUser));
		Long postId = saved.getId();

		postService.deletePost(postId, fixedUser);

		assertThat(postRepository.findById(postId)).isEmpty();
	}

	@Test
	@DisplayName("존재하지 않는 게시글 삭제 시 PostNotFoundException 발생")
	void 존재하지_않는_게시글_삭제_예외() {
		Long nonExistentPostId = 999L;

		assertThatThrownBy(() -> postService.deletePost(nonExistentPostId, fixedUser))
			.isInstanceOf(PostNotFoundException.class);
	}
}