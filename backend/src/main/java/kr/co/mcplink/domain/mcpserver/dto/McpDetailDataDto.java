package kr.co.mcplink.domain.mcpserver.dto;

import lombok.Builder;

@Builder
public record McpDetailDataDto (
    long id,
    String type,
    String url,
    int stars,
    int views,
    boolean official,
    boolean scanned,
    McpServerDetailDto mcpServer
) {

}