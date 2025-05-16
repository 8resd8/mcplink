package kr.co.mcplink.domain.mcpserver.v1.dto.response;

import kr.co.mcplink.domain.mcpserver.v1.dto.McpSummaryDataDto;
import kr.co.mcplink.domain.mcpserver.v1.dto.PageInfoDto;

import java.util.List;

public record McpSearchResponse(
	PageInfoDto pageInfo,
	List<McpSummaryDataDto> mcpServers
) {

}
