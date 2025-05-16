package kr.co.mcplink.domain.mcpserver.v1.dto.response;

import java.util.List;

public record McpTagResponse(
    List<String> mcpTags
) {

}