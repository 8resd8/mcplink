package kr.co.mcplink.domain.github.dto;

import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record ParsedReadmeInfoDto(
        String name,
        String command,
        List<String> args,
        Map<String, String> env
) {

}