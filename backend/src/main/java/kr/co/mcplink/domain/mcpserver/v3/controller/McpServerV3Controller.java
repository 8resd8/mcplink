package kr.co.mcplink.domain.mcpserver.v3.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3/mcp/servers")
@RequiredArgsConstructor
@Tag(name = "McpServer API v3")
public class McpServerV3Controller {

}
