package kr.co.mcplink.domain.post.controller;

import static kr.co.mcplink.global.common.Constants.*;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kr.co.mcplink.domain.post.dto.PostDto;
import kr.co.mcplink.domain.post.dto.request.CreatePostRequest;
import kr.co.mcplink.domain.post.dto.request.UpdatePostRequest;
import kr.co.mcplink.domain.post.service.PostService;
import kr.co.mcplink.domain.user.repository.UserRepository;
import kr.co.mcplink.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/posts")
@RequiredArgsConstructor
public class PostController {

	private final PostService postService;
	private final UserRepository userRepository;

	// 현재 유저는 하드코딩
	@PostMapping
	public ResponseEntity<ApiResponse<Void>> createPost(@Valid @RequestBody CreatePostRequest request) {
		postService.createPost(request, userRepository.findById(1L).get());

		return ResponseEntity
			.status(HttpStatus.CREATED)
			.body(ApiResponse.successNoContent(SUCCESS, "게시글 생성 성공"));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<PostDto>>> getAllPosts() {
		List<PostDto> posts = postService.getAllPosts();

		return ResponseEntity
			.status(HttpStatus.OK)
			.body(ApiResponse.success(SUCCESS, "게시글 목록 조회 성공", posts));
	}

	@GetMapping("/{postId}")
	public ResponseEntity<ApiResponse<PostDto>> getPostById(@PathVariable("postId") Long postId) {
		PostDto post = postService.getPostById(postId);

		return ResponseEntity
			.status(HttpStatus.OK)
			.body(ApiResponse.success(SUCCESS, "게시글 조회 성공", post));
	}

	@PutMapping("/{postId}")
	public ResponseEntity<ApiResponse<PostDto>> updatePost(@PathVariable("postId") Long postId,
		@Valid @RequestBody UpdatePostRequest request) {

		PostDto updatedPostDto = postService.updatePost(postId, request, userRepository.findById(1L).get());

		return ResponseEntity
			.status(HttpStatus.OK)
			.body(ApiResponse.success(SUCCESS, "게시글 수정 성공", updatedPostDto));
	}

	@DeleteMapping("/{postId}")
	public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable("postId") Long postId) {
		postService.deletePost(postId, userRepository.findById(1L).get());

		return ResponseEntity
			.status(HttpStatus.NO_CONTENT)
			.body(ApiResponse.successNoContent(SUCCESS, "게시글 삭제 성공"));
	}
}