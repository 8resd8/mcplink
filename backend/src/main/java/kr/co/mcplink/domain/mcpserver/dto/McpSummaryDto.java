package kr.co.mcplink.domain.mcpserver.dto;

import lombok.Builder;

@Builder
public record McpSummaryDto (
    String name,
    String description
) {

}