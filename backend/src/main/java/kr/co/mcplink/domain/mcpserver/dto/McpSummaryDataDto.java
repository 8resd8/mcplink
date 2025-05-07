package kr.co.mcplink.domain.mcpserver.dto;

import lombok.Builder;

@Builder
public record McpSummaryDataDto (
    long id,
    String type,
    String url,
    int stars,
    int views,
    boolean scanned,
    McpSummaryDto mcpServers
) {

}