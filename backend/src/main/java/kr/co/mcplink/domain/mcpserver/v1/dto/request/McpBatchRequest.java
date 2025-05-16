package kr.co.mcplink.domain.mcpserver.v1.dto.request;

import java.util.List;

public record McpBatchRequest (
    List<Long> serverIds
) {

}