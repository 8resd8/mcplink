package kr.co.mcplink.domain.mcpserver.controller;

import kr.co.mcplink.domain.mcpserver.dto.request.McpBatchRequest;
import kr.co.mcplink.domain.mcpserver.dto.response.*;
import kr.co.mcplink.domain.mcpserver.service.core.McpServerService;
import kr.co.mcplink.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/mcp/servers")
@RequiredArgsConstructor
public class McpServerRestController {

    private final McpServerService mcpServerService;

    @GetMapping
    public ApiResponse<McpListResponse> listServers(
            @RequestParam(required = false, defaultValue = "5") Integer size,
            @RequestParam(required = false, defaultValue = "0") Long cursorId
    ) {
        return mcpServerService.getServers(size, cursorId);
    }

    @GetMapping("/search")
    public ApiResponse<McpSearchResponse> searchByName(
            @RequestParam("name") String name,
            @RequestParam(required = false, defaultValue = "5") Integer size,
            @RequestParam(required = false, defaultValue = "0") Long cursorId
    ) {
        return mcpServerService.searchByName(name, size, cursorId);
    }

    @PostMapping("/batch")
    public ApiResponse<McpBatchResponse> getBatch(
            @RequestParam(required = false, defaultValue = "5") Integer size,
            @RequestParam(required = false, defaultValue = "0") Long cursorId,
            @RequestBody McpBatchRequest batchRequest
    ) {
        List<Long> serverIds = batchRequest.serverIds();
        return mcpServerService.getBatch(serverIds, size, cursorId);
    }

    @GetMapping("/{serverId}")
    public ApiResponse<McpDetailResponse> getDetail(
            @PathVariable("serverId") Long seq
    ) {
        return mcpServerService.getDetail(seq);
    }

    @GetMapping("/tags")
    public ApiResponse<McpTagResponse> listTags() {

        return mcpServerService.listTags();
    }
}