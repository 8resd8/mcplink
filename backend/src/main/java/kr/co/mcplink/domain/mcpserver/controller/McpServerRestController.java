package kr.co.mcplink.domain.mcpserver.controller;

import kr.co.mcplink.domain.mcpserver.dto.response.McpDetailResponse;
import kr.co.mcplink.domain.mcpserver.dto.response.McpListResponse;
import kr.co.mcplink.domain.mcpserver.dto.response.McpSearchResponse;
import kr.co.mcplink.domain.mcpserver.dto.response.McpTagResponse;
import kr.co.mcplink.domain.mcpserver.service.core.McpServerService;
import kr.co.mcplink.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/mcp/servers")
@RequiredArgsConstructor
public class McpServerRestController {

    private final McpServerService mcpServerService;

    @GetMapping
    public ApiResponse<McpListResponse> listAll(
            @RequestParam(required = false, defaultValue = "5") Integer size,
            @RequestParam(required = false, defaultValue = "0") Long cursorId
    ) {
        return mcpServerService.getLists(size, cursorId);
    }

    @GetMapping("/search")
    public ApiResponse<McpSearchResponse> searchByName(
            @RequestParam("name") String name,
            @RequestParam(required = false, defaultValue = "5") Integer size,
            @RequestParam(required = false, defaultValue = "0") Long cursorId
    ) {
        return mcpServerService.searchByName(name, size, cursorId);
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