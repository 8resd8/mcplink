package kr.co.mcplink.domain.mcpserver.v1.dto;

import kr.co.mcplink.domain.mcpserver.v1.entity.SecurityRank;
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
    SecurityRank securityRank,
    McpServerDetailDto mcpServer
) {

}