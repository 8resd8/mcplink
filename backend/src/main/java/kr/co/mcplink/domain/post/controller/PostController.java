package kr.co.mcplink.domain.post.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
import kr.co.mcplink.domain.user.entity.User;
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
	public ApiResponse<Void> createPost(@Valid @RequestBody CreatePostRequest request) {
		postService.createPost(request, userRepository.findById(1L).get());

		return ApiResponse.successNoData(HttpStatus.CREATED.toString(), null);
	}

	@GetMapping
	public ApiResponse<List<PostDto>> getAllPosts() {
		return ApiResponse.success(HttpStatus.OK.toString(), "success", postService.getAllPosts());
	}

	@GetMapping("/{postId}")
	public ApiResponse<PostDto> getPostById(@PathVariable Long postId) {
		return ApiResponse.success(HttpStatus.OK.toString(), "success", postService.getPostById(postId));
	}

	// 게시글 수정
	@PutMapping("/{postId}")
	public ApiResponse<PostDto> updatePost(@PathVariable("postId") Long postId,
		@Valid @RequestBody UpdatePostRequest request) {

		PostDto updatedPostDto = postService.updatePost(postId, request, userRepository.findById(1L).get());

		return ApiResponse.success(HttpStatus.CREATED.toString(), "게시글이 성공적으로 수정되었습니다.", updatedPostDto);
	}

	// 게시글 삭제
	@DeleteMapping("/{postId}")
	public ApiResponse<Void> deletePost(@PathVariable("postId") Long postId) {
		postService.deletePost(postId, userRepository.findById(1L).get());

		return ApiResponse.successNoData(HttpStatus.NO_CONTENT.toString(), null);
	}
}