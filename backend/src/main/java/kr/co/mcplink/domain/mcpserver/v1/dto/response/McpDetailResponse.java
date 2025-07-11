package kr.co.mcplink.domain.mcpserver.v1.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import kr.co.mcplink.domain.mcpserver.v1.dto.McpDetailDataDto;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpDetailResponse (
    McpDetailDataDto mcpServer
) {

}