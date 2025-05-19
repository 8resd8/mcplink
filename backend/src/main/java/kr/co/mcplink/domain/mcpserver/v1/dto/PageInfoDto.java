package kr.co.mcplink.domain.mcpserver.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.Optional;

@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record PageInfoDto(
        Optional<Long> startCursor,
        Optional<Long> endCursor,
        boolean hasNextPage,
        long totalItems
) {

}