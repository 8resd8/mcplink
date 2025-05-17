package kr.co.mcplink.domain.mcpserver.v3.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.mcplink.domain.mcpserver.v1.dto.request.McpBatchRequest;
import kr.co.mcplink.domain.mcpserver.v1.dto.response.*;
import kr.co.mcplink.domain.mcpserver.v3.service.McpServerV3Service;
import kr.co.mcplink.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v3/mcp/servers")
@RequiredArgsConstructor
@Tag(name = "McpServer API v3")
public class McpServerV3Controller {

    private final McpServerV3Service mcpServerService;

    @GetMapping
    public ApiResponse<McpListResponse> getAllServers(
            @RequestParam(required = false, defaultValue = "5") Integer size,
            @RequestParam(required = false, defaultValue = "0") Long cursorId
    ) {
        return mcpServerService.findAllServers(size, cursorId);
    }

    @GetMapping("/search")
    public ApiResponse<McpSearchResponse> getServersByName(
            @RequestParam("name") String name,
            @RequestParam(required = false, defaultValue = "5") Integer size,
            @RequestParam(required = false, defaultValue = "0") Long cursorId
    ) {
        return mcpServerService.searchServersByName(name, size, cursorId);
    }

    @PostMapping("/batch")
    public ApiResponse<McpBatchResponse> getServersByIds(
            @RequestParam(required = false, defaultValue = "5") Integer size,
            @RequestParam(required = false, defaultValue = "0") Long cursorId,
            @RequestBody McpBatchRequest batchRequest
    ) {
        List<Long> serverIds = batchRequest.serverIds();
        return mcpServerService.findServersByIds(serverIds, size, cursorId);
    }

    @GetMapping("/{serverId}")
    public ApiResponse<McpDetailResponse> getServerDetail(
            @PathVariable("serverId") Long seq
    ) {
        return mcpServerService.findServerById(seq);
    }

    @GetMapping("/tags")
    public ApiResponse<McpTagResponse> getAllTags() {

        return mcpServerService.findAllTags();
    }
}
