package kr.co.mcplink.domain.mcpserver.v1.dto;

import lombok.Builder;

@Builder
public record PageInfoDto (
    long startCursor,
    long endCursor,
    boolean hasNextPage,
    long totalItems
) {

}