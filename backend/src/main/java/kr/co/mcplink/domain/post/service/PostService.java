package kr.co.mcplink.domain.post.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import kr.co.mcplink.domain.post.dto.PostDto;
import kr.co.mcplink.domain.post.dto.request.CreatePostRequest;
import kr.co.mcplink.domain.post.dto.request.UpdatePostRequest;
import kr.co.mcplink.domain.post.entity.Post;
import kr.co.mcplink.domain.post.repository.PostRepository;
import kr.co.mcplink.domain.user.entity.User;
import kr.co.mcplink.domain.user.repository.UserRepository;
import kr.co.mcplink.global.exception.PostNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

	private final PostRepository postRepository;
	private final UserRepository userRepository;
	private final EntityManager em;

	// 게시글 생성
	public void createPost(CreatePostRequest request, User user) {
		Post post = Post.createPost(request.title(), request.content(), user);

		postRepository.save(post);
	}

	// 게시글 전체 조회
	public List<PostDto> getAllPosts() {
		return postRepository.findAll()
			.stream()
			.map(PostDto::PostDtoFromEntity)
			.toList();
	}

	// 특정 게시글 조회
	public PostDto getPostById(Long postId) {
		Post post = getPost(postId);

		return PostDto.PostDtoFromEntity(post);
	}

	// 게시글 수정
	public PostDto updatePost(Long postId, UpdatePostRequest request, User user) {
		Post post = getOwnPost(postId, user);

		post.updatePost(request.title(), request.content());
		System.out.println("post.getContent() = " + post.getContent());
		System.out.println("post.getContent() = " + post.getTitle());

		return PostDto.PostDtoFromEntity(post);
	}

	// 게시글 삭제
	public void deletePost(Long postId, User user) {
		Post post = getOwnPost(postId, user);

		postRepository.delete(post);
	}

	private Post getPost(Long postId) {
		return postRepository.findById(postId).orElseThrow(PostNotFoundException::new);
	}

	// 본인 소유의 게시글
	private Post getOwnPost(Long postId, User user) {
		return postRepository.findByIdAndUser(postId, user).orElseThrow(PostNotFoundException::new);
	}
}