package kr.co.mcplink.domain.mcpserver.v3.service;

import kr.co.mcplink.domain.mcpserver.kr.repository.McpServerKrRepository;
import kr.co.mcplink.domain.mcpserver.v1.dto.*;
import kr.co.mcplink.domain.mcpserver.v1.dto.response.*;
import kr.co.mcplink.domain.mcpserver.v3.entity.McpServerV3;
import kr.co.mcplink.domain.mcpserver.v3.repository.McpTagV3Repository;
import kr.co.mcplink.global.common.ApiResponse;
import kr.co.mcplink.global.common.Constants;
import kr.co.mcplink.global.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class McpServerV3Service {

    private final McpServerKrRepository serverRepository;
    private final McpTagV3Repository tagRepository;

    public ApiResponse<McpListResponse> findAllServers(Integer size, Long cursorId) {
        List<McpServerV3> servers = serverRepository.listAll(size, cursorId);

        long total = serverRepository.countAll();
        long endCursor = servers.isEmpty() ? 0L : servers.get(servers.size() - 1).getSeq();
        long remaining = serverRepository.countRemaining(endCursor);

        PageInfoDto pageInfo = PaginationUtil.buildPageInfoV3(servers, total, remaining);

        List<McpSummaryDataDto> mcpServers = servers.stream()
                .map(this::toSummaryDataDto)
                .collect(Collectors.toList());

        McpListResponse response = new McpListResponse(pageInfo, mcpServers);

        return ApiResponse.success(HttpStatus.OK.toString(), Constants.MSG_SUCCESS_LIST, response);
    }

    public ApiResponse<McpSearchResponse> searchServersByName(String name, Integer size, Long cursorId) {
        List<McpServerV3> servers = serverRepository.searchByName(name, size, cursorId);

        long total = serverRepository.countByName(name);
        long endCursor = servers.isEmpty() ? 0L : servers.get(servers.size() - 1).getSeq();
        long remaining = serverRepository.countRemainingByName(name, endCursor);

        PageInfoDto pageInfo = PaginationUtil.buildPageInfoV3(servers, total, remaining);

        List<McpSummaryDataDto> mcpServers = servers.stream()
                .map(this::toSummaryDataDto)
                .collect(Collectors.toList());

        McpSearchResponse response = new McpSearchResponse(pageInfo, mcpServers);

        return ApiResponse.success(HttpStatus.OK.toString(), Constants.MSG_SUCCESS_SEARCH, response);
    }

    public ApiResponse<McpBatchResponse> findServersByIds(List<Long> seqs, Integer size, Long cursorId) {
        List<McpServerV3> servers = new ArrayList<>();

        for (Long seq : seqs) {
            McpServerV3 server = serverRepository.findBySeq(seq).orElse(null);

            if(server == null) {
                return ApiResponse.error(HttpStatus.NOT_FOUND.toString(), Constants.MSG_NOT_FOUNDS);
            }
        }

        List<Long> pageIds = PaginationUtil.slicePageIdsForBatch(seqs, size, cursorId);

        for (Long seq : pageIds) {
            McpServerV3 server = serverRepository.findBySeq(seq).orElse(null);
            servers.add(server);
        }

        PageInfoDto pageInfo = PaginationUtil.buildPageInfoForBatch(seqs, size, cursorId);

        List<McpSummaryDataDto> mcpServers = servers.stream()
                .map(this::toSummaryDataDto)
                .collect(Collectors.toList());

        McpBatchResponse response = new McpBatchResponse(pageInfo, mcpServers);

        return ApiResponse.success(HttpStatus.OK.toString(), Constants.MSG_SUCCESS_BATCH, response);
    }

    public ApiResponse<McpDetailResponse> findServerById(Long seq) {
        McpServerV3 server = serverRepository.findBySeq(seq).orElse(null);

        if(server == null) {
            return ApiResponse.error(HttpStatus.NOT_FOUND.toString(), Constants.MSG_NOT_FOUND);
        }

        serverRepository.findAndIncrementViewsBySeq(seq);

        McpDetailDataDto mcpServer = toDetailDataDto(server);
        McpDetailResponse response = new McpDetailResponse(mcpServer);

        return ApiResponse.success(HttpStatus.OK.toString(), Constants.MSG_SUCCESS_DETAIL, response);
    }

    public ApiResponse<McpTagResponse> findAllTags() {
        List<String> tags = tagRepository.listAll();

        McpTagResponse response = new McpTagResponse(tags);

        return ApiResponse.success(HttpStatus.OK.toString(), Constants.MSG_SUCCESS_TAG_LIST, response);
    }

    private McpSummaryDataDto toSummaryDataDto(McpServerV3 s) {
        return McpSummaryDataDto.builder()
                .id(s.getSeq())
                .type(s.getType())
                .url(s.getUrl())
                .stars(s.getStars())
                .views(s.getViews())
                .official(s.isOfficial())
                .scanned(s.isScanned())
                .securityRank(s.getSecurityRank())
                .mcpServer(
                        McpServerSummaryDto.builder()
                                .name(s.getDetail().getName())
                                .description(s.getDetail().getDescription())
                                .build()
                )
                .build();
    }

    private McpDetailDataDto toDetailDataDto(McpServerV3 s) {
        return McpDetailDataDto.builder()
                .id(s.getSeq())
                .type(s.getType())
                .url(s.getUrl())
                .stars(s.getStars())
                .views(s.getViews())
                .official(s.isOfficial())
                .scanned(s.isScanned())
                .securityRank(s.getSecurityRank())
                .mcpServer(
                        McpServerDetailDto.builder()
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