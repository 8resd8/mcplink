package kr.co.mcplink.domain.mcpserver.service.core;

import kr.co.mcplink.domain.mcpserver.dto.*;
import kr.co.mcplink.domain.mcpserver.dto.response.McpDetailResponse;
import kr.co.mcplink.domain.mcpserver.dto.response.McpListResponse;
import kr.co.mcplink.domain.mcpserver.dto.response.McpSearchResponse;
import kr.co.mcplink.domain.mcpserver.dto.response.McpTagResponse;
import kr.co.mcplink.domain.mcpserver.entity.McpServer;
import kr.co.mcplink.domain.mcpserver.repository.McpServerRepository;
import kr.co.mcplink.domain.mcpserver.repository.McpTagRepository;
import kr.co.mcplink.global.common.ApiResponse;
import kr.co.mcplink.global.common.Constants;
import kr.co.mcplink.global.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class McpServerService {
	private final McpServerRepository serverRepository;
	private final McpTagRepository tagRepository;
	private final McpServerRepository mcpServerRepository;

	public ApiResponse<McpListResponse> getLists(Integer size, Long cursorId) {
		List<McpServer> items = serverRepository.listAll(size, cursorId);

		long total = serverRepository.countAll();
		long endCursor = items.isEmpty() ? 0L : items.get(items.size() - 1).getSeq();
		long remaining = serverRepository.countRemaining(endCursor);

		PageInfoDto pageInfo = PaginationUtil.buildPageInfo(items, total, remaining);

		List<McpSummaryDataDto> data = items.stream()
			.map(this::toSummaryDataDto)
			.collect(Collectors.toList());

		McpListResponse response = new McpListResponse(pageInfo, data);

		return ApiResponse.success(HttpStatus.OK.toString(), Constants.MSG_SUCCESS_LIST, response);
	}

	public ApiResponse<McpSearchResponse> searchByName(String name, Integer size, Long cursorId) {
		List<McpServer> items = serverRepository.searchByName(name, size, cursorId);

		long total = serverRepository.countByName(name);
		long endCursor = items.isEmpty() ? 0L : items.get(items.size() - 1).getSeq();
		long remaining = serverRepository.countRemainingByName(name, endCursor);

		PageInfoDto pageInfo = PaginationUtil.buildPageInfo(items, total, remaining);

		List<McpSummaryDataDto> data = items.stream()
		        .map(this::toSummaryDataDto)
		        .collect(Collectors.toList());

		McpSearchResponse response = new McpSearchResponse(pageInfo, data);

		return ApiResponse.success(HttpStatus.OK.toString(), Constants.MSG_SUCCESS_SEARCH, response);
	}

	public ApiResponse<McpDetailResponse> getDetail(Long seq) {
		McpServer server = serverRepository.findBySeq(seq).orElse(null);

		if(server == null) {
			return ApiResponse.error(HttpStatus.NOT_FOUND.toString(), Constants.MSG_NOT_FOUND);
		}

		mcpServerRepository.incrementViews(seq);

		McpDetailDataDto dto = toDetailDataDto(server);
		McpDetailResponse response = new McpDetailResponse(dto);

		return ApiResponse.success(HttpStatus.OK.toString(), Constants.MSG_SUCCESS_DETAIL, response);
	}

	public ApiResponse<McpTagResponse> listTags() {
		List<String> items = tagRepository.listAll();

		McpTagResponse response = new McpTagResponse(items);

		return ApiResponse.success(HttpStatus.OK.toString(), Constants.MSG_SUCCESS_TAG_LIST, response);
	}

	private McpSummaryDataDto toSummaryDataDto(McpServer s) {
		return McpSummaryDataDto.builder()
			.id(s.getSeq())
			.type(s.getType())
			.url(s.getUrl())
			.stars(s.getStars())
			.views(s.getViews())
			.scanned(s.isScanned())
			.mcpServers(
				McpSummaryDto.builder()
					.name(s.getDetail().getName())
					.description(s.getDetail().getDescription())
					.build()
			)
			.build();
	}

	private McpDetailDataDto toDetailDataDto(McpServer s) {
		return McpDetailDataDto.builder()
			.id(s.getSeq())
			.type(s.getType())
			.url(s.getUrl())
			.stars(s.getStars())
			.views(s.getViews())
			.scanned(s.isScanned())
			.mcpServers(
				McpDetailDto.builder()
					.name(s.getDetail().getName())
					.description(s.getDetail().getDescription())
					.command(s.getDetail().getCommand())
					.args(s.getDetail().getArgs())
					.env(s.getDetail().getEnv())
					.build()
			)
			.build();
	}
}