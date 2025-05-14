package kr.co.mcplink.domain.github.model;

import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record ParsedReadmeInfo(
        String name,
        String command,
        List<String> args,
        Map<String, String> env
) {

}