package kr.co.mcplink.domain.mcpserver.v1.dto;

import lombok.Builder;

@Builder
public record McpServerSummaryDto (
    String name,
    String description
) {

}