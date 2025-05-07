package kr.co.mcplink.domain.mcpserver.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import kr.co.mcplink.domain.mcpserver.dto.McpDetailDataDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpDetailResponse (
    McpDetailDataDto data
) {

}